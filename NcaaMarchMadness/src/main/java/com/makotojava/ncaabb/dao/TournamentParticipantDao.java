package com.makotojava.ncaabb.dao;

import com.makotojava.ncaabb.model.TournamentParticipant;

public interface TournamentParticipantDao {

  /**
   * Fetch and return the TournamentParticipant object for
   * the specified team and year.
   */
  TournamentParticipant fetchByYear(final String teamName, final Integer year);
}
