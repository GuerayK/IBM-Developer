package com.makotojava.ncaabb.simulation;

import com.makotojava.ncaabb.generation.CsvTrainingDataCreator;
import com.makotojava.ncaabb.model.SeasonData;
import com.makotojava.ncaabb.springconfig.ApplicationConfig;
import com.makotojava.ncaabb.util.NetworkUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TournamentCsvDataGenerator extends TournamentMatrixPredictor {
  /**
   * Constructor.
   *
   * @param applicationContext The Spring ApplicationContext object.
   */
  public TournamentCsvDataGenerator(final ApplicationContext applicationContext) {
    super(applicationContext);
  }

  public static void main(final String[] args) {
    if (args.length < 1) {
      usage();
      System.exit(-1);
    }
    //
    // Get the tournament year
    Integer year = Integer.valueOf(args[0]);

    //
    // Validate the year
    NetworkUtils.validateYear(year);
    //
    // Instantiate the class and handoff
    TournamentCsvDataGenerator simulator = new TournamentCsvDataGenerator(
      new AnnotationConfigApplicationContext(ApplicationConfig.class));
    //
    // Let's go.
    simulator.go(year);
  }

  /**
   * The "do it" method. Drives the entire matrix production.
   * Pretty cool, man.
   *
   * @param tournamentYear
   */
  public void go(Integer tournamentYear) {
    //
    // Get the list of all teams participating in the tournament for the specified year
    Set<String> teamNames = fetchTournamentTeams(tournamentYear);

    //
    // Now write out a line of data with features in the correct order
    // for every team playing every other team. This data will be fed into
    // the trained model for making predictions
    for (String teamName: teamNames) {
      SeasonData teamSeasonData = pullSeasonData(tournamentYear, teamName);
      List<List<Double>> results = new ArrayList<>();
      //
      // Now pretend the current team is playing every other team
      for (String opponentTeamName: teamNames) {
        //
        // A team can't play itself, silly
        if (opponentTeamName.equals(teamName)) {
          continue;
        }
        SeasonData opponentSeasonData = pullSeasonData(tournamentYear, opponentTeamName);
        //
        // Generate data
        List<Double> csvData = CsvTrainingDataCreator.writeSeasonData(teamSeasonData, opponentSeasonData, null);
        results.add(csvData);
      }
      //
      // Write the data
      String filename = NetworkUtils.computeDl4jCsvSimulationDataFileName(tournamentYear, teamName);
      CsvTrainingDataCreator.saveCsvFileWithoutResults(filename, results);
    }
  }

}
