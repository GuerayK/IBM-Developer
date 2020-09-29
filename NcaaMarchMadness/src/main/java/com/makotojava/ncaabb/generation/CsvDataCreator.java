package com.makotojava.ncaabb.generation;

import com.makotojava.ncaabb.model.SeasonData;
import com.makotojava.ncaabb.model.TournamentResult;
import com.makotojava.ncaabb.springconfig.ApplicationConfig;
import com.makotojava.ncaabb.util.NetworkUtils;
import com.opencsv.CSVWriter;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CsvDataCreator extends DataCreator {
  private static final Logger log = Logger.getLogger(DataCreator.class);

  /**
   * Constructor.
   *
   * @param applicationContext The Spring ApplicationContext object that
   *                           contains the environment.
   */
  public CsvDataCreator(final ApplicationContext applicationContext) {
    super(applicationContext);
  }

  public static void main(final String[] args) {
    if (args.length < 1) {
      usage();
      System.exit(-1);
    }
    //
    // Let's kick the tires and light the fires
    CsvDataCreator trainingDataCreator = new CsvDataCreator(
      new AnnotationConfigApplicationContext(ApplicationConfig.class));
    //
    // First let's figure out what year(s) we are running.
    Integer[] yearsForTraining = trainingDataCreator.computeYearsToTrain(args);
    log.info("*********** CREATING CSV DATA **************");
    log.info(
      "Using data from the following years for CSV: " + ReflectionToStringBuilder.toString(yearsForTraining));
    //
    // Now create the data
    trainingDataCreator.go(yearsForTraining);
  }

  public void go(Integer[] yearsForTraining) {
    List<List<Double>> dataRows = new ArrayList<>();
    //
    // Now we create the training data for those years
    for (Integer year : yearsForTraining) {
      //
      // Pull the current year's tournament results
      List<TournamentResult> tournamentResults = pullTournamentResults(year);
      //
      // Loop through each game played in the current year's tournament.
      // Pull the season data for both winner and loser.
      // Create a row of training data with the winner as the LHS of the data, loser as RHS.
      // Create another row that is just the opposite.
      // The idea is to correct for positional bias in the model. If the winner's data
      /// is always on the LHS, then whatever data is run in the final simulation/predication
      /// on the LHS will be the winner. However, for the true prediction, we don't know
      /// who the winner is, because the games haven't been played.
      for (TournamentResult tournamentResult : tournamentResults) {
        //
        // Each tournament game
        String winningTeamName = tournamentResult.getWinningTeamName();
        String losingTeamName = tournamentResult.getLosingTeamName();
        SeasonData seasonDataWinning = pullSeasonData(year, winningTeamName);
        SeasonData seasonDataLosing = pullSeasonData(year, losingTeamName);
        // Attempt to eliminate bias in the position
        List<Double> rowWin = writeSeasonData(seasonDataWinning, seasonDataLosing, 1.0);
        List<Double> rowLoss = writeSeasonData(seasonDataLosing, seasonDataWinning, 0.0);
        dataRows.add(rowWin);
        dataRows.add(rowLoss);
      }
      if (log.isTraceEnabled()) {
        log.trace("Dumping out training data:");
        for (List<Double> row : dataRows) {
          log.trace("Row: " + ReflectionToStringBuilder.toString(row));
        }
      }
    }
    // Shuffle the data
    Collections.shuffle(dataRows);
    log.info("*********** SAVING CSV DATA **************");
    String filename = NetworkUtils.computeDl4jCsvDataFileName(yearsForTraining);
    saveCsvFile(filename, dataRows);
    int numberOfRows = dataRows.size();
    log.info("Saved " + numberOfRows + " rows of CSV data to file: '" + filename + "'");
  }

  private List<Double> writeSeasonData(final SeasonData seasonDataWinning,
                                       final SeasonData seasonDataLosing,
                                       final Double result) {
    List<Double> ret = new ArrayList<>();
    // Offense
    ret.add(seasonDataWinning.getAvgPointsPerGame().doubleValue());
    ret.add(seasonDataWinning.getScoringMarginPerGame().doubleValue());
    ret.add(seasonDataWinning.getNumFgAttemptsPerGame().doubleValue());
    ret.add(seasonDataWinning.getFgPercentage().doubleValue());
    ret.add(seasonDataWinning.getNum3pPerGame().doubleValue());
    ret.add(seasonDataWinning.getNum3pAttemptsPerGame().doubleValue());
    ret.add(seasonDataWinning.getT3pPercentage().doubleValue());
    ret.add(seasonDataWinning.getNumFtAttemptsPerGame().doubleValue());
    ret.add(seasonDataWinning.getFtPercentage().doubleValue());
    ret.add(seasonDataWinning.getReboundMargin().doubleValue());
    ret.add(seasonDataWinning.getAssistsPerGame().doubleValue());
    ret.add(seasonDataWinning.getAtoRatio().doubleValue());
    // Defense
    ret.add(seasonDataWinning.getAvgOpponentPointsPerGame().doubleValue());
    ret.add(seasonDataWinning.getNumOppFgAttemptsPerGame().doubleValue());
    ret.add(seasonDataWinning.getOppFgPercentage().doubleValue());
    ret.add(seasonDataWinning.getNumOpp3pAttemptsPerGame().doubleValue());
    ret.add(seasonDataWinning.getOpp3pPercentage().doubleValue());
    ret.add(seasonDataWinning.getBlocksPerGame().doubleValue());
    ret.add(seasonDataWinning.getStealsPerGame().doubleValue());
    ret.add(seasonDataWinning.getOppTurnoversPerGame().doubleValue());
    // Errors
    ret.add(seasonDataWinning.getTurnoversPerGame().doubleValue());
    ret.add(seasonDataWinning.getFoulsPerGame().doubleValue());
    ret.add(seasonDataWinning.getNumDq().doubleValue());
    // Offense
    ret.add(seasonDataLosing.getAvgPointsPerGame().doubleValue());
    ret.add(seasonDataLosing.getScoringMarginPerGame().doubleValue());
    ret.add(seasonDataLosing.getNumFgAttemptsPerGame().doubleValue());
    ret.add(seasonDataLosing.getFgPercentage().doubleValue());
    ret.add(seasonDataLosing.getNum3pPerGame().doubleValue());
    ret.add(seasonDataLosing.getNum3pAttemptsPerGame().doubleValue());
    ret.add(seasonDataLosing.getT3pPercentage().doubleValue());
    ret.add(seasonDataLosing.getNumFtAttemptsPerGame().doubleValue());
    ret.add(seasonDataLosing.getFtPercentage().doubleValue());
    ret.add(seasonDataLosing.getReboundMargin().doubleValue());
    ret.add(seasonDataLosing.getAssistsPerGame().doubleValue());
    ret.add(seasonDataLosing.getAtoRatio().doubleValue());
    // Defense
    ret.add(seasonDataLosing.getAvgOpponentPointsPerGame().doubleValue());
    ret.add(seasonDataLosing.getNumOppFgAttemptsPerGame().doubleValue());
    ret.add(seasonDataLosing.getOppFgPercentage().doubleValue());
    ret.add(seasonDataLosing.getNumOpp3pAttemptsPerGame().doubleValue());
    ret.add(seasonDataLosing.getOpp3pPercentage().doubleValue());
    ret.add(seasonDataLosing.getBlocksPerGame().doubleValue());
    ret.add(seasonDataLosing.getStealsPerGame().doubleValue());
    ret.add(seasonDataLosing.getOppTurnoversPerGame().doubleValue());
    // Errors
    ret.add(seasonDataLosing.getTurnoversPerGame().doubleValue());
    ret.add(seasonDataLosing.getFoulsPerGame().doubleValue());
    ret.add(seasonDataLosing.getNumDq().doubleValue());
    // Class - Win (1.0) or Lose (0.0)
    ret.add(result);
    return ret;
  }

  private void saveCsvFile(final String filename, final List<List<Double>> dataRows) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
      try (CSVWriter csvWriter = new CSVWriter(writer)) {
        dataRows.forEach(row -> {
          String[] outputLine = new String[row.size()];
          int index = 0;
          for (Double value : row) {
            outputLine[index++] = value.toString();
          }
          csvWriter.writeNext(outputLine);
        });
      }
    } catch (IOException e) {
      String errorMessage = String.format("IOException occurred writing file '%s': %s", filename, e.getLocalizedMessage());
      log.error(errorMessage, e);
      throw new RuntimeException(errorMessage, e);
    }
  }

}
