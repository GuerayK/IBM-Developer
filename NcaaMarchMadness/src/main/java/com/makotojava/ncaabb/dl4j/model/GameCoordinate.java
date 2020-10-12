package com.makotojava.ncaabb.dl4j.model;

import java.util.Objects;

public class GameCoordinate {
  private final TeamCoordinate home;
  private final TeamCoordinate away;

  public GameCoordinate(final TeamCoordinate home, final TeamCoordinate away) {
    this.home = home;
    this.away = away;
  }

  public TeamCoordinate getHome() {
    return home;
  }

  public TeamCoordinate getAway() {
    return away;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final GameCoordinate that = (GameCoordinate) o;
    return Objects.equals(home, that.home) &&
      Objects.equals(away, that.away);
  }

  @Override
  public int hashCode() {
    return Objects.hash(home, away);
  }

  @Override
  public String toString() {
    return "GameCoordinate{" +
      "home=" + home +
      ", away=" + away +
      '}';
  }
}
