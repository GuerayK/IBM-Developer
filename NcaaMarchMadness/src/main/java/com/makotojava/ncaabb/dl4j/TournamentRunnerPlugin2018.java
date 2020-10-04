package com.makotojava.ncaabb.dl4j;

import com.makotojava.ncaabb.model.SeasonData;
import com.makotojava.ncaabb.util.NetworkUtils;
import org.springframework.context.ApplicationContext;

import java.io.File;
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
  public String getTournamentTemplateFileName() {
    return NetworkUtils.fetchSimulationDirectoryAndCreateIfNecessary() + File.separator + "tournament-template-ncaabb-2018.txt";
  }

  @Override
  protected Map<TeamCoordinate, SeasonData> buildTeamCoordinateMap() {
    // Map a team to its initial team coordinates
    Map<TeamCoordinate, SeasonData> coordinateSeasonDataMap = new HashMap<>();
    // First Four
    createFirstFourMappings(coordinateSeasonDataMap);
    // Round of 64 (Round 1)
    createRoundOneMappingsRegion0(coordinateSeasonDataMap);
    createRoundOneMappingsRegion1(coordinateSeasonDataMap);
    createRoundOneMappingsRegion2(coordinateSeasonDataMap);
    createRoundOneMappingsRegion3(coordinateSeasonDataMap);
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

  // For 2018 Region 0 is the South
  private void createRoundOneMappingsRegion0(final Map<TeamCoordinate, SeasonData> coordinateSeasonDataMap) {
//    [0,1,0],[0,1,1]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 0, "Virginia");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 1, "UMBC");
//    [0,1,2],[0,1,3]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 2, "Creighton");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 3, "Kansas St");
//    [0,1,4],[0,1,5]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 4, "Kentucky");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 5, "Davidson");
//    [0,1,6],[0,1,7]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 6, "Arizona");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 7, "Buffalo");
//    [0,1,8],[0,1,9]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 8, "Miami FL");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 9, "Loyola Chicago");
//    [0,1,10],[0,1,11]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 10, "Tennessee");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 11, "Wright St");
//    [0,1,12],[0,1,13]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 12, "Nevada");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 13, "Texas");
//    [0,1,14],[0,1,15]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 14, "Cincinnati");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 15, "Georgia St");
  }

  // For 2018 Region 1 is the West
  private void createRoundOneMappingsRegion1(final Map<TeamCoordinate, SeasonData> coordinateSeasonDataMap) {
//    [1,1,0],[1,1,1]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 0, "Xavier");
    // [1,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [1,1,2],[1,1,3]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 2, "Missouri");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 3, "Florida St");
//    [1,1,4],[1,1,5]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 4, "Ohio St");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 5, "South Dakota St");
//    [1,1,6],[1,1,7]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 6, "Gonzaga");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 7, "UNC Greensboro");
//    [1,1,8],[1,1,9]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 8, "Houston");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 9, "San Diego St");
//    [1,1,10],[1,1,11]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 10, "Michigan");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 11, "Montana");
//    [1,1,12],[1,1,13]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 12, "Texas AM");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 13, "Providence");
//    [1,1,14],[1,1,15]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 14, "North Carolina");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 15, "Lipscomb");
  }

  // For 2018 Region 2 is the East
  private void createRoundOneMappingsRegion2(final Map<TeamCoordinate, SeasonData> coordinateSeasonDataMap) {
//    [2,1,0],[2,1,1]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 0, "Villanova");
    // [2,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [2,1,2],[2,1,3]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 2, "Virginia Tech");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 3, "Alabama");
//    [2,1,4],[2,1,5]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 4, "West Virginia");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 5, "Murray St");
//    [2,1,6],[2,1,7]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 6, "Wichita St");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 7, "Marshall");
//    [2,1,8],[2,1,9]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 8, "Florida");
    // [2,1,9] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [2,1,10],[2,1,11]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 10, "Texas Tech");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 11, "SFA");
//    [2,1,12],[2,1,13]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 12, "Arkansas");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 13, "Butler");
//    [2,1,14],[2,1,15]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 14, "Purdue");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 15, "Cal St Fullerton");
  }

  // For 2019 Region 3 is the Midwest
  private void createRoundOneMappingsRegion3(final Map<TeamCoordinate, SeasonData> coordinateSeasonDataMap) {
//    [3,1,0],[3,1,1]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 0, "Kansas");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 1, "Penn");
//    [3,1,2],[3,1,3]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 2, "Seton Hall");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 3, "NC State");
//    [3,1,4],[3,1,5]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 4, "Clemson");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 5, "New Mexico St");
//    [3,1,6],[3,1,7]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 6, "Auburn");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 7, "Col of Charleston");
//    [3,1,8],[3,1,9]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 8, "TCU");
    // [3,1,9] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [3,1,10],[3,1,11]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 10, "Michigan St");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 11, "Bucknell");
//    [3,1,12],[3,1,13]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 12, "Rhode Island");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 13, "Oklahoma");
//    [3,1,14],[3,1,15]
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 14, "Duke");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 15, "Iona");
  }

}
