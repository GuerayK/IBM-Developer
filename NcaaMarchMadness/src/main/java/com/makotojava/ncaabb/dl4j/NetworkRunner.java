package com.makotojava.ncaabb.dl4j;

import com.makotojava.ncaabb.util.NetworkUtils;
import org.apache.log4j.Logger;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class NetworkRunner {

  private static final Logger log = Logger.getLogger(NetworkRunner.class);

  public static final String DL4J_NETWORK_EPILOGUE = "dl4j.zip";

  public static void go(final Scanner scanner, final List<NetworkCandidate> networkCandidateList) {
    //
    // Load saved networks
    List<NetworkCandidate> savedNetworks = loadNetworks();
    //
    // Add loaded networks to networkCandidateList
    List<NetworkCandidate> allNetworks = new ArrayList<>();
    allNetworks.addAll(savedNetworks);
    allNetworks.addAll(networkCandidateList);
    //
    // Ask the user what they want to do
    promptUser(scanner, allNetworks);
  }

  private static List<NetworkCandidate> loadNetworks() {
    List<NetworkCandidate> ret = new ArrayList<>();
    String networkDirectoryName = NetworkUtils.fetchNetworkDirectoryAndCreateIfNecessary();
    File networkDirectory = new File(networkDirectoryName);
    //
    // Get a listing of *.dl4j.zip
    String[] dl4jNetworkFileNames = networkDirectory.list((dir, name) -> name.endsWith(DL4J_NETWORK_EPILOGUE));
    if (dl4jNetworkFileNames != null && dl4jNetworkFileNames.length > 0) {
      String[] networkFileNames = new String[dl4jNetworkFileNames.length];
      log.info(String.format("Found %d networks at %s", dl4jNetworkFileNames.length, networkDirectoryName));
      int index = 0;
      //
      // Need the full path to the network file
      for (String dl4jNetworkFileName: dl4jNetworkFileNames) {
        networkFileNames[index++] = networkDirectoryName + File.separator + dl4jNetworkFileName;
      }
      ret = loadNetworkFiles(networkFileNames);
    } else {
      log.warn(String.format("No networks found at %s", networkDirectoryName));
    }
    return ret;
  }

  private static void promptUser(final Scanner scanner, final List<NetworkCandidate> allNetworks) {
  }

  private static List<NetworkCandidate> loadNetworkFiles(final String[] networkFileNames) {
    List<NetworkCandidate> ret = new ArrayList<>();
    for (String networkFileName: networkFileNames) {
      //
      // Load the network
      File networkFile = new File(networkFileName);
      try {
        MultiLayerNetwork network = ModelSerializer.restoreMultiLayerNetwork(networkFile, true);
        if (network == null) {
          log.error(String.format("Network from file %s is null!", networkFileName));
          continue; // Do not add null network
        }
        NetworkParameters networkParameters = ModelSerializer.getObjectFromFile(networkFile, NetworkPersister.NETWORK_PARAMETERS_KEY);
        if (networkParameters == null) {
          log.error(String.format("Network parameters from file %s is null!", networkFileName));
          continue; // Do not add null network parameters
        }
        //
        // Everything looks good, add the loaded network to the return value
        ret.add(new NetworkCandidate(networkParameters, network));
      } catch (IOException e) {
        String message = String.format("Error loading network: %s", networkFileName);
        log.error(message, e);
      }
    }
    return ret;
  }

}
