package com.makotojava.ncaabb.dao;

import com.makotojava.ncaabb.model.TournamentResult;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentResultFlatFileDao implements TournamentResultDao {

  private static final Logger log = Logger.getLogger(TournamentResultFlatFileDao.class);

  private static final int NUMBER_OF_FIELDS = 7;
  private static final String FILE_NAME = "/tournament_result.csv";

  private Map<Integer, List<TournamentResult>> database = new HashMap<>();

  public TournamentResultFlatFileDao() {
    loadData(database);
  }

  private static void loadData(final Map<Integer, List<TournamentResult>> database) {
    String databaseFileName = FILE_NAME;
    InputStream inputStream = TournamentResultFlatFileDao.class.getResourceAsStream(databaseFileName);
    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
      CSVReader csvReader = new CSVReader(bufferedReader);
      csvReader.readNext(); // Read header
      String[] line = csvReader.readNext();
      while (line != null) {
        if (line.length == NUMBER_OF_FIELDS) {
          TournamentResult data = parseLine(line);
          List<TournamentResult> tournamentResults = database.computeIfAbsent(data.getYear(), k -> new ArrayList<>());
          tournamentResults.add(data);
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

  private static TournamentResult parseLine(final String[] line) {
    TournamentResult ret = new TournamentResult();
    ret.setId(Integer.parseInt(line[0]));
    ret.setYear(Integer.parseInt(line[1]));
    ret.setGameDate(Date.from(LocalDate.parse(line[2]).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
    ret.setWinningTeamName(StringUtils.trim(line[3]));
    ret.setWinningScore(Integer.parseInt(line[4]));
    ret.setLosingTeamName(StringUtils.trim(line[5]));
    ret.setLosingScore(Integer.parseInt(line[6]));
    return ret;
  }

  @Override
  public List<TournamentResult> fetchAllByYear(final Integer year) {
    return database.get(year);
  }
}
