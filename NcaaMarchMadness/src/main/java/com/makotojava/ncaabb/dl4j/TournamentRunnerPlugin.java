package com.makotojava.ncaabb.dl4j;

import com.makotojava.ncaabb.dao.SeasonDataDao;
import com.makotojava.ncaabb.model.SeasonData;
import com.makotojava.ncaabb.util.NetworkUtils;
import org.apache.log4j.Logger;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.springframework.context.ApplicationContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;

/**
 * This is the base class for running networks for a specific tournament.
 * This class is subclassed and key methods are overridden to create a
 * tournament-specific plugin that can be used to make predictions.
 */
public abstract class TournamentRunnerPlugin {

  private static final Logger log = Logger.getLogger(TournamentRunnerPlugin.class);

  private static final Byte KEEP_LOOPING = -1;

  private Map<TeamCoordinate, SeasonData> teamCoordinateSeasonDataMap;

  private SeasonDataDao seasonDataDao;

  public TournamentRunnerPlugin(final ApplicationContext applicationContext) {
    this.seasonDataDao = applicationContext.getBean(SeasonDataDao.class);
  }

  /**
   * Return the tournament year handled by the plugin subclass
   */
  protected abstract Integer getTournamentYear();

  /**
   * Build the TeamCoordinate->SeasonData map, specific to a tournament year.
   */
  protected abstract Map<TeamCoordinate, SeasonData> buildTeamCoordinateMap();

  /**
   * Return the name of the tournament template file. Defaults to a 68 team
   * NCAA Men's basketball-style tournament (a.k.a., March Madness).
   *
   * The tournament template file is used to setup the games that will
   * be played in the tournament. Each game has a set of coordinates that
   * correspond to the region, round, and teams that participate in the
   * game. The region and round are the same for any given game from year
   * to year, but the teams that participate will be different and are
   * specified by the subclass that is for the tournament year that plugin
   * was written for.
   */
  public String getTournamentTemplateFileName() {
    return NetworkUtils.fetchSimulationDirectoryAndCreateIfNecessary() + File.separator + "./tournament-template-ncaabb.txt";
  }

  /**
   * Fetch and return season data for the team specified in the TeamCoordinate object
   * that is passed to this method.
   */
  public SeasonData fetchSeasonData(final TeamCoordinate teamCoordinate) {
    return seasonDataDao.fetchByYearAndTeamName(getTournamentYear(), teamCoordinate.getName());
  }

  /**
   * Create the list of games that will be played during the tournament.
   * A game is specified by its GameCoordinate object, since each game
   * can occur in a different round, region, and played by different
   * teams.
   */
  public List<GameCoordinate> createTournamentGameList() {
    List<GameCoordinate> ret = new ArrayList<>();
    String tournamentFileName = getTournamentTemplateFileName();
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(tournamentFileName))) {
      String line = bufferedReader.readLine();
      while (line != null) {
        if (!line.trim().startsWith("#")) {
          GameCoordinate gameCoordinate = parseTournamentTemplateLine(line);
          ret.add(gameCoordinate);
        }
        line = bufferedReader.readLine();
      }
    } catch (IOException e) {
      String message = String.format("Error occurred while processing tournament template file: %s: %s", tournamentFileName, e.getLocalizedMessage());
      log.error(message, e);
    }
    return ret;
  }

  /**
   * Using the specified list of GameCoordinates, run the tournament.
   */
  public void runTournament(final List<GameCoordinate> gameList, final NetworkCandidate networkCandidate) {
    gameList.forEach(gameCoordinate -> {
      //
      // Get the home team from the GameCoordinate
      TeamCoordinate homeTeamCoordinate = gameCoordinate.getHome();
      SeasonData homeTeamSeasonData = getTeamCoordinateSeasonDataMap().get(homeTeamCoordinate);
      TeamCoordinate awayTeamCoordinate = gameCoordinate.getAway();
      SeasonData awayTeamSeasonData = getTeamCoordinateSeasonDataMap().get(awayTeamCoordinate);
      //
      // Now compute two sets of records to eliminate positional bias
      MultiLayerNetwork network = networkCandidate.getMultiLayerNetwork();
      //
      // Compute RecordReaderDataSetIterator from the two sets of season data
      RecordReaderDataSetIterator recordReaderDataSetIterator = computeRecordIterator(homeTeamSeasonData, awayTeamSeasonData);
      INDArray indArrayResults = network.output(recordReaderDataSetIterator);
      //
      // Figure out who we declare the winner and compute their TeamCoordinates according to the algorithm
      TeamCoordinate winnerTeamCoordinate = computeWinner(indArrayResults, homeTeamCoordinate, awayTeamCoordinate);
    });
  }

  private TeamCoordinate computeWinner(final INDArray indArrayResults,
                                       final TeamCoordinate homeTeamCoordindate,
                                       final TeamCoordinate awayTeamCoordindate) {
    TeamCoordinate ret = new TeamCoordinate();
    double[][] results = indArrayResults.toDoubleMatrix();
    //
    // First row is with home team first
    double home1WinProbability = results[0][0];
    double away1WinProbability = results[0][1];
    boolean home1Win = home1WinProbability > away1WinProbability;
    // Next row is with home team second
    double home2WinProbability = results[1][0];
    double away2WinProbability = results[1][1];
    boolean home2Win = home2WinProbability > away2WinProbability;
    // Set attributes on the new TeamCoordinate object
    ret.setRegion(homeTeamCoordindate.getRegion());
    ret.setRound(homeTeamCoordindate.getRound() + 1);
    if (homeTeamCoordindate.getRound() == 0) {
      ret.setIndex(awayTeamCoordindate.getIndex());
    } else {
      ret.setIndex(homeTeamCoordindate.getIndex() / 2);
    }
    //
    // If (home1Win and home2Win) or (!home1Win and !home2Win), then symmetric, else flip a coin
    if (home1Win && home2Win) {
      // Symmetric win - winner: home team
      SeasonData seasonData = getTeamCoordinateSeasonDataMap().get(homeTeamCoordindate);
      ret.setName(seasonData.getTeamName());
      getTeamCoordinateSeasonDataMap().put(ret,seasonData);
    } else if (!home1Win && !home2Win) {
      // Symmetric loss - winner: away team
      SeasonData seasonData = getTeamCoordinateSeasonDataMap().get(awayTeamCoordindate);
      ret.setName(seasonData.getTeamName());
      getTeamCoordinateSeasonDataMap().put(ret, seasonData);
    } else {
      // Flip a coin
      TeamCoordinate coinTossWinner = flipCoin(homeTeamCoordindate, awayTeamCoordindate);
      SeasonData seasonData = getTeamCoordinateSeasonDataMap().get(coinTossWinner);
      ret.setName(seasonData.getTeamName());
      getTeamCoordinateSeasonDataMap().put(ret, seasonData);
    }
    return ret;
  }

  /**
   * Sometimes ya just gotta flip a coin.
   */
  private TeamCoordinate flipCoin(final TeamCoordinate homeTeamCoordindate, final TeamCoordinate awayTeamCoordindate) {
    int numberOfHomeTeamWins = 0;
    int numberOfAwayTeamWins = 0;
    for (int aa = 0; aa < 10000; aa++) {
      Random random = new Random();
      int randomInt = random.nextInt(10000);
      if (randomInt > 4999) {
        numberOfHomeTeamWins++;
      } else {
        numberOfAwayTeamWins++;
      }
    }
    if (numberOfAwayTeamWins > numberOfHomeTeamWins) {
      log.warn(String.format("Home team wins coin toss: %s", homeTeamCoordindate));
      return awayTeamCoordindate;
    } else {
      log.warn(String.format("Away team wins coin toss: %s", awayTeamCoordindate));
      return homeTeamCoordindate;
    }
  }

  protected RecordReaderDataSetIterator computeRecordIterator(final SeasonData homeTeamSeasonData,
                                                              final SeasonData awayTeamSeasonData) {
    //
    // Compute one row of data with home team first
    //
    // Compute another row of data with away team first
    //
    // Wrap both rows in a CSVRecordReader, with all the bells and whistles and return the appropriate iterator
    return null;
  }

  protected void createTeamCoordinateMapping(final Map<TeamCoordinate, SeasonData> coordinateSeasonDataMap,
                                             final int region,
                                             final int round,
                                             final int index,
                                             final String teamName) {
    TeamCoordinate teamCoordinate = new TeamCoordinate()
      .setName(teamName)
      .setIndex(index)
      .setRegion(region)
      .setRound(round);
    SeasonData seasonData = fetchSeasonData(teamCoordinate);
    coordinateSeasonDataMap.put(teamCoordinate, seasonData);
  }

  public static Optional<TournamentRunnerPlugin> selectPluginToRunMenu(final Scanner scanner,
                                                                       final List<TournamentRunnerPlugin> tournamentRunnerPlugins) {
    Optional<TournamentRunnerPlugin> ret = Optional.empty();
    // Ask the user which plugin they want to run and for what year, and if it's doable, return the plugin
    byte pluginNumber = KEEP_LOOPING;
    while (pluginNumber == KEEP_LOOPING && !tournamentRunnerPlugins.isEmpty()) {
      System.out.println("Enter the number of the network you want to persist (enter 0 to quit):");
      System.out.println("Plugin#\t\tTournament Year");
      int index = 0;
      for (TournamentRunnerPlugin tournamentRunnerPlugin : tournamentRunnerPlugins) {
        System.out.printf("   %d %24s%n", index + 1, tournamentRunnerPlugin.getTournamentYear().toString());
        index++;
      }
      if (scanner.hasNextByte()) {
        pluginNumber = scanner.nextByte();
        if (pluginNumber < 0 || pluginNumber > tournamentRunnerPlugins.size()) {
          pluginNumber = KEEP_LOOPING;
          continue;
        }
        if (pluginNumber == 0) {
          break;
        }
      } else {
        System.out.printf("%s is not a valid choice.%n", scanner.next());
        pluginNumber = KEEP_LOOPING;
      }
    }
    if (pluginNumber != 0) {
      ret = Optional.of(tournamentRunnerPlugins.get(pluginNumber - 1));
    }
    return ret;
  }

  public static GameCoordinate parseTournamentTemplateLine(final String line) {
    // A line looks like this:
    // [0,0,0],[0,0,1]
    // TODO: Code me up
    return null;
  }

  public Map<TeamCoordinate, SeasonData> getTeamCoordinateSeasonDataMap() {
    return teamCoordinateSeasonDataMap;
  }

  public TournamentRunnerPlugin setTeamCoordinateSeasonDataMap(final Map<TeamCoordinate, SeasonData> teamCoordinateSeasonDataMap) {
    this.teamCoordinateSeasonDataMap = teamCoordinateSeasonDataMap;
    return this;
  }
}
