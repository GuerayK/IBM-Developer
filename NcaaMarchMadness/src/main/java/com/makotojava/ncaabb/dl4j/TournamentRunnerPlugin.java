package com.makotojava.ncaabb.dl4j;

import com.makotojava.ncaabb.dao.SeasonDataDao;
import com.makotojava.ncaabb.model.SeasonData;
import com.makotojava.ncaabb.util.NetworkUtils;
import org.apache.log4j.Logger;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.InputStreamInputSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.shade.guava.io.CharStreams;
import org.springframework.context.ApplicationContext;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * This is the base class for running networks for a specific tournament.
 * This class is subclassed and key methods are overridden to create a
 * tournament-specific plugin that can be used to make predictions.
 */
public abstract class TournamentRunnerPlugin {

  private static final Logger log = Logger.getLogger(TournamentRunnerPlugin.class);

  private static final Byte KEEP_LOOPING = -1;
  private static final int BATCH_SIZE = 2000;

  private Map<TeamCoordinate, SeasonData> teamCoordinateSeasonDataMap;

  private SeasonDataDao seasonDataDao;

  public TournamentRunnerPlugin(final ApplicationContext applicationContext) {
    this.seasonDataDao = applicationContext.getBean(SeasonDataDao.class);
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

  static Optional<GameCoordinate> parseTournamentTemplateLine(final String line) {
    Optional<GameCoordinate> ret = Optional.empty();
    // A line looks like this:
    // [0,0,0],[0,0,1]
    StringTokenizer strtok = new StringTokenizer(line.trim(), "[]vs");
    List<String> coordinates = new ArrayList<>();
    while (strtok.hasMoreElements()) {
      String element = strtok.nextToken();
      coordinates.add(element);
    }
    // There must be two components (home and away), or something is wrong
    if (coordinates.size() == 2) {
      TeamCoordinate homeTeamCoordinates = TeamCoordinate.parseCoordinates(coordinates.get(0));
      TeamCoordinate awayTeamCoordinates = TeamCoordinate.parseCoordinates(coordinates.get(1));
      ret = Optional.of(new GameCoordinate(homeTeamCoordinates, awayTeamCoordinates));
    } else {
      String message = String.format("Cannot process malformed tournament template file line: '%s'", line);
      log.error(message);
      throw new RuntimeException(message);
    }
    return ret;
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
   * <p>
   * The tournament template file is used to setup the games that will
   * be played in the tournament. Each game has a set of coordinates that
   * correspond to the region, round, and teams that participate in the
   * game. The region and round are the same for any given game from year
   * to year, but the teams that participate will be different and are
   * specified by the subclass that is for the tournament year that plugin
   * was written for.
   */
  public String getTournamentTemplateFileName() {
    return NetworkUtils.fetchSimulationDirectoryAndCreateIfNecessary() + File.separator + "tournament-template-ncaabb.txt";
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
          Optional<GameCoordinate> gameCoordinateMaybe = parseTournamentTemplateLine(line);
          gameCoordinateMaybe.ifPresent(ret::add);
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
      Optional<RecordReaderDataSetIterator> recordReaderDataSetIteratorMaybe = computeRecordIterator(homeTeamSeasonData, awayTeamSeasonData, networkCandidate.getNetworkParameters());
      recordReaderDataSetIteratorMaybe.ifPresent(readerDataSetIterator -> {
        INDArray indArrayResults = network.output(recordReaderDataSetIteratorMaybe.get());
        //
        // Figure out who we declare the winner and compute their TeamCoordinates according to the algorithm
        Optional<TeamCoordinate> winnerTeamCoordinateMaybe = computeWinner(indArrayResults.toDoubleMatrix(), homeTeamCoordinate, awayTeamCoordinate);
        winnerTeamCoordinateMaybe.ifPresent(teamCoordinate -> log.info(String.format("Winner: %s", teamCoordinate)));
      });
    });
  }

  /**
   * Compute the winner by reasonable means, and failing that, flip a coin.
   * <p>
   * This can get really confusing really fast.
   * <p>
   * The data coming in (in the INDArray) is made up of two rows of data
   * in the internal double[][]:
   * <p>
   * Row 0 - the network's pick with the home team on the LHS and the away team on the RHS
   * Row 1 - the network's pick with the away team on the LHS and the home team on the RHS
   * <p>
   * There are two elements in each double[]:
   * Element 0 - the probability of the LHS being the winner
   * Element 1 - the probability of the RHS being the winner
   * <p>
   * Obviously, Elements 0 and 1 are interpreted differently depending on whether we
   * are looking at Row 0 (home team as LHS) or Row 1 (away team as LHS).
   * <p>
   * The variables in this method are named with the home and away teams
   * in mind to make the code (hopefully) easier to understand.
   *
   * @return TeamCoordinate - the new team coordinates of the winner, chosen as described above.
   */
  private Optional<TeamCoordinate> computeWinner(final double[][] results,
                                                 final TeamCoordinate homeTeamCoordinate,
                                                 final TeamCoordinate awayTeamCoordinate) {
    // Set attributes on the new TeamCoordinate object
    Optional<TeamCoordinate> ret = Optional.empty();
    SeasonData homeSeasonData = getTeamCoordinateSeasonDataMap().get(homeTeamCoordinate);
    String homeTeamName = homeSeasonData.getTeamName(); // FOR DEBUGGING
    SeasonData awaySeasonData = getTeamCoordinateSeasonDataMap().get(awayTeamCoordinate);
    String awayTeamName = awaySeasonData.getTeamName(); // FOR DEBUGGING
    //
    // Process network's pick.
    // The numbers in the variables indicate the Row number they are referencing
    // For example, homeWinProbability is the probability from Row 0
    // of a home win (that is, the LHS probability is > than the RHS probability).
    //
    // First row is with home team first
    double home0WinProbability = results[0][0]; // home from row 0
    double away0WinProbability = results[0][1]; // away from row 0
    boolean home0Win = home0WinProbability > away0WinProbability;
    boolean away0Win = !home0Win; // Hopefully makes it easier to follow
    // Next row is with home team second (switch the order)
    double away1WinProbability = results[1][0]; // away from row 1
    double home1WinProbability = results[1][1]; // home from row 1
    boolean home1Win = home1WinProbability > away1WinProbability;
    boolean away1Win = !home1Win;
    double home0WinCertainty = home0WinProbability - away0WinProbability;
    double away0WinCertainty = away0WinProbability - home0WinProbability;
    double home1WinCertainty = home1WinProbability - away1WinProbability;
    double away1WinCertainty = away1WinProbability - home1WinProbability;
    //
    // If (home0Win and home1Win) or (!home0Win and !home1Win), then symmetric
    if (home0Win && home1Win) {
      // Symmetric home win - winner: home team
      ret = setWinner(homeSeasonData, homeTeamCoordinate, awayTeamCoordinate);
    } else if (away0Win && away1Win) {
      // Symmetric away win - winner: away team
      ret = setWinner(awaySeasonData, homeTeamCoordinate, awayTeamCoordinate);
      // Now we deal with asymmetries.
    } else if (home0WinCertainty > away1WinCertainty) {
      // home win and network more sure of that than away win
      ret = setWinner(homeSeasonData, homeTeamCoordinate, awayTeamCoordinate);
    } else if (away0WinCertainty > home1WinCertainty) {
      // home loss and network more sure of that than away loss
      ret = setWinner(awaySeasonData,homeTeamCoordinate, awayTeamCoordinate);
    }
    return ret;
  }

  private Optional<TeamCoordinate> setWinner(final SeasonData winnerSeasonData,
                                             final TeamCoordinate homeTeamCoordinate,
                                             final TeamCoordinate awayTeamCoordindate) {
    Optional<TeamCoordinate> ret = Optional.empty();
    TeamCoordinate teamCoordinate = computeNextRoundCoordindates(homeTeamCoordinate, awayTeamCoordindate, winnerSeasonData);
    teamCoordinate.setName(winnerSeasonData.getTeamName());
    getTeamCoordinateSeasonDataMap().put(teamCoordinate, winnerSeasonData);
    ret = Optional.of(teamCoordinate);
    return ret;
  }

  private TeamCoordinate computeNextRoundCoordindates(final TeamCoordinate homeTeamCoordinate,
                                                      final TeamCoordinate awayTeamCoordindate,
                                                      final SeasonData winnerSeasonData) {
    TeamCoordinate ret = new TeamCoordinate();
    switch (homeTeamCoordinate.getRound()) {
      case 0:
        ret.setIndex(awayTeamCoordindate.getIndex());
        ret.setRegion(homeTeamCoordinate.getRegion());
        break;
      case 1:
      case 2:
      case 3:
      case 4:
        ret.setIndex(homeTeamCoordinate.getIndex() / 2);
        ret.setRegion(homeTeamCoordinate.getRegion() * 2 / 2);
        break;
      case 5:
        ret.setIndex(homeTeamCoordinate.getIndex() / 2);
        ret.setRegion(homeTeamCoordinate.getRegion() / 2);
        break;
    }
    ret.setRound(homeTeamCoordinate.getRound() + 1);
    return ret;
  }

  /**
   * Sometimes ya just gotta flip a coin.
   */
  private TeamCoordinate flipCoin(final TeamCoordinate homeTeamCoordinate, final TeamCoordinate awayTeamCoordindate) {
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
      log.warn(String.format("Home team wins coin toss: %s", homeTeamCoordinate));
      return awayTeamCoordindate;
    } else {
      log.warn(String.format("Away team wins coin toss: %s", awayTeamCoordindate));
      return homeTeamCoordinate;
    }
  }

  protected Optional<RecordReaderDataSetIterator> computeRecordIterator(final SeasonData homeTeamSeasonData,
                                                                        final SeasonData awayTeamSeasonData,
                                                                        final NetworkParameters networkParameters) {
    Optional<RecordReaderDataSetIterator> ret = Optional.empty();
    //
    // Compute one row of data with home team first
    String[] homeFirst = networkParameters.transformRow(NetworkTrainer.writeSeasonData(homeTeamSeasonData, awayTeamSeasonData, null), false);
    //
    // Compute another row of data with away team first
    String[] awayFirst = networkParameters.transformRow(NetworkTrainer.writeSeasonData(awayTeamSeasonData, homeTeamSeasonData, null), false);
    //
    // Wrap both rows in a CSVRecordReader, with all the bells and whistles and return the appropriate iterator
    StringBuilder stringSource = new StringBuilder();
    String homeFirstString = String.join(",", homeFirst);
    stringSource.append(homeFirstString);
    stringSource.append("\n");
    String awayFirstString = String.join(",", awayFirst);
    stringSource.append(awayFirstString);
    stringSource.append("\n");
    StringReader stringReader = new StringReader(stringSource.toString());
    try {
      InputStream inputStream = new ByteArrayInputStream(CharStreams.toString(stringReader).getBytes());
      RecordReader tournamentReader = new CSVRecordReader(0, ',');
      InputStreamInputSplit split = new InputStreamInputSplit(inputStream);
      tournamentReader.initialize(split);
      RecordReaderDataSetIterator tournamentIterator = new RecordReaderDataSetIterator(tournamentReader, BATCH_SIZE);
      ret = Optional.of(tournamentIterator);
    } catch (IOException | InterruptedException e) {
      String message = String.format("Error creating InputStream from CSV data:\n%s", stringSource.toString());
      log.error(message, e);
      throw new RuntimeException(e);
    }
    return ret;
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

  public Map<TeamCoordinate, SeasonData> getTeamCoordinateSeasonDataMap() {
    return teamCoordinateSeasonDataMap;
  }

  public TournamentRunnerPlugin setTeamCoordinateSeasonDataMap(final Map<TeamCoordinate, SeasonData> teamCoordinateSeasonDataMap) {
    this.teamCoordinateSeasonDataMap = teamCoordinateSeasonDataMap;
    return this;
  }
}
