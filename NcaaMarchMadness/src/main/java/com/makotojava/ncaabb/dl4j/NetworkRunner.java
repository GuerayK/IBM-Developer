package com.makotojava.ncaabb.dl4j;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class NetworkRunner {

  private static final Logger log = Logger.getLogger(NetworkRunner.class);

  public static void go(final Scanner scanner, final List<NetworkCandidate> networkCandidateList) {
    //
    // Load saved networks
    List<NetworkCandidate> savedNetworks = NetworkUtils.loadNetworks();
    //
    // Add loaded networks to networkCandidateList
    List<NetworkCandidate> allNetworks = new ArrayList<>();
    allNetworks.addAll(savedNetworks);
    allNetworks.addAll(networkCandidateList);
    if (!allNetworks.isEmpty()) {
      //
      // Load plugins
      List<TournamentRunnerPlugin> tournamentRunnerPlugins = loadPlugins();
      if (!tournamentRunnerPlugins.isEmpty()) {
        //
        // Ask the user what they want to do
        promptUser(scanner, allNetworks, tournamentRunnerPlugins);
      } else {
        log.error("No TournamentRunnerPlugins, cannot run a network to simulate a tournament!");
      }
    } else {
      log.info("No networks to run. Train some networks and try again.");
    }
  }

  private static List<TournamentRunnerPlugin> loadPlugins() {
    List<TournamentRunnerPlugin> ret = Arrays.asList(
      //new TournamentRunnerPlugin2019(MainMenu.getApplicationContext())
      new TournamentRunnerPlugin2018(MainMenu.getApplicationContext())
    );

    return ret;
  }

  private static void promptUser(final Scanner scanner,
                                 final List<NetworkCandidate> allNetworks,
                                 final List<TournamentRunnerPlugin> tournamentRunnerPlugins) {
    //
    // Get the network the user wants to run
    Optional<NetworkCandidate> selectedNetwork = NetworkPersister.displayNetworkSelectionMenu(scanner, allNetworks, NetworkPersister.ACTION_WORK_WITH);
    if (selectedNetwork.isPresent()) {
      NetworkCandidate networkCandidate = selectedNetwork.get();
      NetworkParameters networkParameters = networkCandidate.getNetworkParameters();
      //
      // Prompt the user for the tournament plugin they want to run
      Optional<TournamentRunnerPlugin> tournamentRunnerPluginMaybe = TournamentRunnerPlugin.selectPluginToRunMenu(scanner, tournamentRunnerPlugins);
      if (tournamentRunnerPluginMaybe.isPresent()) {
        TournamentRunnerPlugin tournamentRunnerPlugin = tournamentRunnerPluginMaybe.get();
        //
        // If the network is valid for the specified tournament year, then run the tournament
        if (networkParameters.isValidYearForTournamentPrediction(tournamentRunnerPlugin.getTournamentYear())) {
          List<GameCoordinate> gameList = tournamentRunnerPlugin.createTournamentGameList();
          tournamentRunnerPlugin.runTournament(gameList, networkCandidate);
        }
      }
    } else {
      System.out.println("No network selected, quitting.");
    }
  }

}
