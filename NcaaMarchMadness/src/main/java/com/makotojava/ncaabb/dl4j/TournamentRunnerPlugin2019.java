package com.makotojava.ncaabb.dl4j;

import com.makotojava.ncaabb.model.SeasonData;
import com.makotojava.ncaabb.util.NetworkUtils;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TournamentRunnerPlugin2019 extends TournamentRunnerPlugin {

  public TournamentRunnerPlugin2019(final ApplicationContext applicationContext) {
    super(applicationContext);
    setTeamCoordinateSeasonDataMap(buildTeamCoordinateMap());
  }

  @Override
  public Integer getTournamentYear() {
    return 2019;
  }

  @Override
  public String getTournamentTemplateFileName() {
    return NetworkUtils.fetchSimulationDirectoryAndCreateIfNecessary() + File.separator + "tournament-template-ncaabb-2019.txt";
  }

  /**
   * Builds the internal map of TeamCoordinate -> SeasonData
   */
  @Override
  protected Map<TeamCoordinate, SeasonData> buildTeamCoordinateMap() {
    // Map a team to its initial team coordinates
    Map<TeamCoordinate, SeasonData> coordinateSeasonDataMap = new HashMap<>();
    // TODO: Layla writes this code
    // [0,0,0],[0,0,1] maps to Prairie View vs Fairleigh Dickinson
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 0, "Prairie View");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 1, "Fairleigh Dickinson");
    // TODO: Add other matchups here
    return coordinateSeasonDataMap;
  }

}
