package com.makotojava.ncaabb.dao;

import com.makotojava.ncaabb.model.TournamentParticipant;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TournamentParticipantJdbcDao extends JdbcDaoSupport implements TournamentParticipantDao {
  private static final Logger log = Logger.getLogger(TournamentResultJdbcDao.class);

  private static final String TABLE_NAME = "tournament_participant";

  public TournamentParticipantJdbcDao(DataSource dataSource) {
    this.setDataSource(dataSource);
  }

  @Override
  public TournamentParticipant fetchByYear(final String teamName, final Integer year) {
    String sql = "SELECT t.* FROM " + TABLE_NAME + " t WHERE t.year = ? AND t.team_name = ? ORDER BY t.team_name";
    Object[] args = {year, teamName};
    // Run the query
    List<TournamentParticipant> results = getJdbcTemplate().query(sql, new TournamentParticipantRowMapper(), args);
    return results.get(0);
  }

  public static class TournamentParticipantRowMapper implements RowMapper<TournamentParticipant> {
    @Override
    public TournamentParticipant mapRow(final ResultSet resultSet, final int i) throws SQLException {
      TournamentParticipant ret = new TournamentParticipant();
      ret.setId(resultSet.getInt("id"));
      ret.setNumberOfVictories(resultSet.getInt("number_of_victories"));
      ret.setTeamName(resultSet.getString("team_name"));
      ret.setYear(resultSet.getInt("year"));

      return ret;
    }
  }
}
