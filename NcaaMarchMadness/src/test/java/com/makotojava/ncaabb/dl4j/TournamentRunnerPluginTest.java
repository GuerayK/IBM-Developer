package com.makotojava.ncaabb.dl4j;

import com.makotojava.ncaabb.dl4j.model.GameCoordinate;
import com.makotojava.ncaabb.dl4j.model.TeamCoordinate;
import com.makotojava.ncaabb.dl4j.plugins.TournamentRunnerPlugin;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentRunnerPluginTest {

  @Test
  void parseTournamentTemplateLine() {
    String line = "[2,3,4]vs[5,6,7]";
    TeamCoordinate expectedHomeTeamCoordinate = new TeamCoordinate().setRegion(2).setRound(3).setIndex(4);
    TeamCoordinate expectedAwayTeamCoordinate = new TeamCoordinate().setRegion(5).setRound(6).setIndex(7);
    GameCoordinate expected = new GameCoordinate(expectedHomeTeamCoordinate, expectedAwayTeamCoordinate);
    Optional<GameCoordinate> actualMaybe = TournamentRunnerPlugin.parseTournamentTemplateLine(line);
    assertTrue(actualMaybe.isPresent());
    assertEquals(expected, actualMaybe.get());

    line = "[0,1,0]vs[0,1,1]";
    expectedHomeTeamCoordinate = new TeamCoordinate().setRegion(0).setRound(1).setIndex(0);
    expectedAwayTeamCoordinate = new TeamCoordinate().setRegion(0).setRound(1).setIndex(1);
    expected = new GameCoordinate(expectedHomeTeamCoordinate, expectedAwayTeamCoordinate);
    actualMaybe = TournamentRunnerPlugin.parseTournamentTemplateLine(line);
    assertTrue(actualMaybe.isPresent());
    assertEquals(expected, actualMaybe.get());

    line = "     [0,1,0]vs[0,1,1]     ";
    expectedHomeTeamCoordinate = new TeamCoordinate().setRegion(0).setRound(1).setIndex(0);
    expectedAwayTeamCoordinate = new TeamCoordinate().setRegion(0).setRound(1).setIndex(1);
    expected = new GameCoordinate(expectedHomeTeamCoordinate, expectedAwayTeamCoordinate);
    actualMaybe = TournamentRunnerPlugin.parseTournamentTemplateLine(line);
    assertTrue(actualMaybe.isPresent());
    assertEquals(expected, actualMaybe.get());
  }

  @Test
  void parseTournamentTemplateLine_RuntimeException() {
    assertThrows(RuntimeException.class, () -> TournamentRunnerPlugin.parseTournamentTemplateLine("[0,1,0]|[0,1,1]"));

    assertThrows(RuntimeException.class, () -> TournamentRunnerPlugin.parseTournamentTemplateLine("   |  [0,1,0]vs[0,1,1]     "));

    assertThrows(RuntimeException.class, () -> TournamentRunnerPlugin.parseTournamentTemplateLine("   []  [0,1,0]vs[0,1,1]     "));

    assertThrows(RuntimeException.class, () -> TournamentRunnerPlugin.parseTournamentTemplateLine("[0,1,0]|[0,1,1][0,1,2]"));
  }
}