package com.makotojava.ncaabb.dl4j;

import com.makotojava.ncaabb.dl4j.model.NetworkCandidate;
import com.makotojava.ncaabb.dl4j.model.NetworkParameters;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NetworkUtils {

  private static final Logger log = Logger.getLogger(NetworkUtils.class);

  public static final String DL4J_NETWORK_EPILOGUE = "dl4j.zip";

  public static List<NetworkCandidate> loadNetworks() {
    List<NetworkCandidate> ret = new ArrayList<>();
    String networkDirectoryName = com.makotojava.ncaabb.util.NetworkUtils.fetchNetworkDirectoryAndCreateIfNecessary();
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

  public static void displayNetworkList(final List<NetworkCandidate> networkCandidates) {
    System.out.printf("%9s%21s%11s(%%)%35s%10s%45s%30s%15s%n",
      "Network #", "When Trained", "Accuracy", "Layer Structure", "Saved?", "Years (Training)", "Updater", "No. Epochs");
    int index = 0;
    for (NetworkCandidate networkCandidate : networkCandidates) {
      NetworkParameters networkParameters = networkCandidate.getNetworkParameters();
      System.out.printf("%9d%21s%13.2f%%%35s%10s%45s%30s%15d%n",
        index + 1,
        networkParameters.getWhenTrained().format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm")),
        networkParameters.getNetworkAccuracy() * 100.0,
        StringUtils.abbreviate(networkParameters.getNetworkLayout(), 32),
        networkParameters.isNetworkSaved() ? "Y": "N",
        networkParameters.getTrainingYearsString(),
        StringUtils.abbreviate(networkParameters.getUpdater().toString(), 24),
        networkParameters.getNumberOfEpochs());
      index++;
    }
  }

  public static void displayNetworks() throws IOException {
    BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
    List<NetworkCandidate> networkCandidates = loadNetworks();
    while (true) {
      displayNetworkList(networkCandidates);
      System.out.println("Enter the number of a network to see more about it (just press Enter to quit).");
      String line = scanner.readLine();
      if (StringUtils.isEmpty(line)) {
        break;
      }
      if (StringUtils.isNumeric(line)) {
        int networkNumber = Integer.parseInt(line);
        if (networkNumber > 0 && networkNumber <= networkCandidates.size()) {
          System.out.println(networkCandidates.get(networkNumber - 1).getNetworkParameters());
        } else {
          System.out.printf("%d is not one of the choices. Please choose a network from the list (or just press enter to quit).%n", networkNumber);
        }
      } else {
        System.out.printf("%s is not a valid choice. Please choose a network from the list (or just press enter to quit).%n", line);
      }
    }
    System.out.println("Quitting.");
  }

}
