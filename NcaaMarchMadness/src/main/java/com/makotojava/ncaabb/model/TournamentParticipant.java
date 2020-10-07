package com.makotojava.ncaabb.model;

public class TournamentParticipant {
  private Integer id;
  private Integer year;
  private String teamName;
  private Integer numberOfVictories;

  public Integer getId() {
    return id;
  }

  public TournamentParticipant setId(final Integer id) {
    this.id = id;
    return this;
  }

  public Integer getYear() {
    return year;
  }

  public TournamentParticipant setYear(final Integer year) {
    this.year = year;
    return this;
  }

  public String getTeamName() {
    return teamName;
  }

  public TournamentParticipant setTeamName(final String teamName) {
    this.teamName = teamName;
    return this;
  }

  public Integer getNumberOfVictories() {
    return numberOfVictories;
  }

  public TournamentParticipant setNumberOfVictories(final Integer numberOfVictories) {
    this.numberOfVictories = numberOfVictories;
    return this;
  }

}
