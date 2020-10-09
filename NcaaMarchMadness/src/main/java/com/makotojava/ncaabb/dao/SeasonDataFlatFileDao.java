package com.makotojava.ncaabb.dao;

import com.makotojava.ncaabb.model.SeasonData;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SeasonDataFlatFileDao implements SeasonDataDao {

  private static final Logger log = Logger.getLogger(SeasonDataFlatFileDao.class);

  private static final int NUMBER_OF_FIELDS = 25;
  private static final String FILE_NAME = "/season_data.csv";

  private Map<Integer, Map<String, SeasonData>> database = new HashMap<>();


  public SeasonDataFlatFileDao() {
    loadData(database);
  }

  private static void loadData(Map<Integer, Map<String, SeasonData>> database) {
    String databaseFileName = FILE_NAME;
    InputStream inputStream = SeasonDataFlatFileDao.class.getResourceAsStream(databaseFileName);
    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
      CSVReader csvReader = new CSVReader(bufferedReader);
      csvReader.readNext(); // Read header
      String[] line = csvReader.readNext();
      while (line != null) {
        if (line.length == NUMBER_OF_FIELDS) {
          SeasonData seasonData = parseLine(line);
          Map<String, SeasonData> teamSeasonDataMap = database.computeIfAbsent(seasonData.getYear(), k -> new HashMap<>());
          teamSeasonDataMap.put(seasonData.getTeamName(), seasonData);
        } else {
          throw new RuntimeException(String.format("Expected %d fields, got %d instead.", NUMBER_OF_FIELDS, line.length));
        }
        line = csvReader.readNext();
      }
    } catch (IOException e) {
      String message = String.format("Error reading database file name '%s': %s", databaseFileName, e.getLocalizedMessage());
      log.error(message, e);
      throw new RuntimeException(message, e);
    }
  }

  @Override
  public List<SeasonData> fetchAllByYear(final Integer year) {
    Map<String, SeasonData> teamSeasonDataMap = database.get(year);
    List<SeasonData> ret = teamSeasonDataMap.keySet()
      .stream()
      .map(teamSeasonDataMap::get)
      .collect(Collectors.toList());
    log.debug(String.format("fetchAllByYear(%d): Returning %d records.", year, ret.size()));
    return ret;
  }

  @Override
  public SeasonData fetchByYearAndTeamName(final Integer year, final String teamName) {
    Map<String, SeasonData> teamSeasonDataMap = database.get(year);
    return teamSeasonDataMap.get(teamName);
  }

  private static SeasonData parseLine(final String[] line) {
    SeasonData seasonData = new SeasonData();
    seasonData.setYear(Integer.parseInt(line[0]));
    seasonData.setTeamName(StringUtils.trim(line[1]));
    // Offense
    seasonData.setAvgPointsPerGame(BigDecimal.valueOf(Double.parseDouble(nice(line[2]))));
    seasonData.setScoringMarginPerGame(BigDecimal.valueOf(Double.parseDouble(nice(line[3]))));
    seasonData.setNumFgAttemptsPerGame(BigDecimal.valueOf(Double.parseDouble(nice(line[4]))));
    seasonData.setFgPercentage(BigDecimal.valueOf(Double.parseDouble(nice(line[5]))));
    seasonData.setNum3pPerGame(BigDecimal.valueOf(Double.parseDouble(nice(line[6]))));
    seasonData.setNum3pAttemptsPerGame(BigDecimal.valueOf(Double.parseDouble(nice(line[7]))));
    seasonData.setT3pPercentage(BigDecimal.valueOf(Double.parseDouble(nice(line[8]))));
    seasonData.setNumFtAttemptsPerGame(BigDecimal.valueOf(Double.parseDouble(nice(line[9]))));
    seasonData.setFtPercentage(BigDecimal.valueOf(Double.parseDouble(nice(line[10]))));
    seasonData.setReboundMargin(BigDecimal.valueOf(Double.parseDouble(nice(line[11]))));
    seasonData.setAssistsPerGame(BigDecimal.valueOf(Double.parseDouble(nice(line[12]))));
    seasonData.setAtoRatio(BigDecimal.valueOf(Double.parseDouble(nice(line[13]))));
    // Defense
    seasonData.setAvgOpponentPointsPerGame(BigDecimal.valueOf(Double.parseDouble(nice(line[14]))));
    seasonData.setNumOppFgAttemptsPerGame(BigDecimal.valueOf(Double.parseDouble(nice(line[15]))));
    seasonData.setOppFgPercentage(BigDecimal.valueOf(Double.parseDouble(nice(line[16]))));
    seasonData.setNumOpp3pAttemptsPerGame(BigDecimal.valueOf(Double.parseDouble(nice(line[17]))));
    seasonData.setOpp3pPercentage(BigDecimal.valueOf(Double.parseDouble(nice(line[18]))));
    seasonData.setBlocksPerGame(BigDecimal.valueOf(Double.parseDouble(nice(line[19]))));
    seasonData.setStealsPerGame(BigDecimal.valueOf(Double.parseDouble(nice(line[20]))));
    seasonData.setOppTurnoversPerGame(BigDecimal.valueOf(Double.parseDouble(nice(line[21]))));
    // Errors
    seasonData.setTurnoversPerGame(BigDecimal.valueOf(Double.parseDouble(nice(line[22]))));
    seasonData.setFoulsPerGame(BigDecimal.valueOf(Double.parseDouble(nice(line[23]))));
    seasonData.setNumDq(BigDecimal.valueOf(Double.parseDouble(nice(line[24]))));

    return seasonData;
  }

  private static String nice(final String string) {
    String ret = "0";
    if (StringUtils.isNotEmpty(string)) {
      ret = StringUtils.trim(string);
    }
    return ret;
  }

}
