package com.makotojava.ncaabb.dl4j;

import com.makotojava.ncaabb.model.SeasonData;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

public class TournamentRunnerPlugin2018 extends TournamentRunnerPlugin {
  public TournamentRunnerPlugin2018(final ApplicationContext applicationContext) {
    super(applicationContext);
    setTeamCoordinateSeasonDataMap(buildTeamCoordinateMap());
  }

  @Override
  protected Integer getTournamentYear() {
    return 2018;
  }

  @Override
  protected Map<TeamCoordinate, SeasonData> buildTeamCoordinateMap() {
    // Map a team to its initial team coordinates
    Map<TeamCoordinate, SeasonData> coordinateSeasonDataMap = new HashMap<>();
    // First Four
    createFirstFourMappings(coordinateSeasonDataMap);
    // Round of 64 (Round 1)
    createRoundOneMappingsRegion0(coordinateSeasonDataMap);
    // TODO: Add other matchups here
    return coordinateSeasonDataMap;
  }

  private void createFirstFourMappings(final Map<TeamCoordinate, SeasonData> coordinateSeasonDataMap) {
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 0, "NC Central");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 1, "Texas Southern");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 0, 0, "LIU Brooklyn");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 0, 1, "Radford");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 0, 8, "St Bonaventure");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 0, 9, "UCLA");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 8, "Arizona St");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 9, "Syracuse");
  }

  private void createRoundOneMappingsRegion0(final Map<TeamCoordinate, SeasonData> coordinateSeasonDataMap) {
//    [0,1,0],[0,1,1]
//    [0,1,2],[0,1,3]
//    [0,1,4],[0,1,5]
//    [0,1,6],[0,1,7]
//    [0,1,8],[0,1,9]
//    [0,1,10],[0,1,11]
//    [0,1,12],[0,1,13]
//    [0,1,14],[0,1,15]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 0, "TODO");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 1, "TODO");
  }

}
