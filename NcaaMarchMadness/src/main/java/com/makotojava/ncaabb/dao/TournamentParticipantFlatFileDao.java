package com.makotojava.ncaabb.dao;

import com.makotojava.ncaabb.model.TournamentParticipant;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class TournamentParticipantFlatFileDao implements TournamentParticipantDao {

  private static final Logger log = Logger.getLogger(TournamentParticipantFlatFileDao.class);

  private static final int NUMBER_OF_FIELDS = 4;
  private static final String FILE_NAME = "/tournament_participant.csv";

  private Map<Integer, Map<String, TournamentParticipant>> database = new HashMap<>();

  public TournamentParticipantFlatFileDao() {
    loadData(database);
  }

  private static void loadData(final Map<Integer, Map<String, TournamentParticipant>> database) {
    String databaseFileName = FILE_NAME;
    InputStream inputStream = TournamentParticipantFlatFileDao.class.getResourceAsStream(databaseFileName);
    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
      CSVReader csvReader = new CSVReader(bufferedReader);
      csvReader.readNext(); // Read header
      String[] line = csvReader.readNext();
      while (line != null) {
        if (line.length == NUMBER_OF_FIELDS) {
          TournamentParticipant data = parseLine(line);
          Map<String, TournamentParticipant> teamDataMap = database.computeIfAbsent(data.getYear(), k -> new HashMap<>());
          teamDataMap.put(data.getTeamName(), data);
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

  private static TournamentParticipant parseLine(final String[] line) {
    TournamentParticipant ret = new TournamentParticipant();
    ret.setId(Integer.parseInt(line[0]));
    ret.setYear(Integer.parseInt(line[1]));
    ret.setTeamName(StringUtils.trim(line[2]));
    ret.setNumberOfVictories(Integer.parseInt(line[3]));
    return ret;
  }

  @Override
  public TournamentParticipant fetchByYear(final String teamName, final Integer year) {
    TournamentParticipant ret = null;
    Map<String, TournamentParticipant> teamDataMap = database.get(year);
    if (teamDataMap != null) {
      ret = teamDataMap.get(teamName);
    }
    return ret;
  }
}