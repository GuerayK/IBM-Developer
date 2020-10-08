package com.makotojava.ncaabb.dl4j;

import com.makotojava.ncaabb.dao.SeasonDataDao;
import com.makotojava.ncaabb.dao.TournamentParticipantDao;
import com.makotojava.ncaabb.dao.TournamentResultDao;
import com.makotojava.ncaabb.dl4j.menus.ActivationFunctionMenuChoice;
import com.makotojava.ncaabb.dl4j.menus.DataElementMenuChoice;
import com.makotojava.ncaabb.dl4j.menus.UpdaterMenuChoice;
import com.makotojava.ncaabb.dl4j.menus.WeightInitMenuChoice;
import com.makotojava.ncaabb.generation.Networks;
import com.makotojava.ncaabb.model.SeasonData;
import com.makotojava.ncaabb.model.TournamentParticipant;
import com.makotojava.ncaabb.model.TournamentResult;
import com.makotojava.ncaabb.util.NetworkUtils;
import com.opencsv.CSVWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.datavec.api.records.metadata.RecordMetaData;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.InputStreamInputSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.shade.guava.io.CharStreams;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.StringTokenizer;

public class NetworkTrainer {

  private static final Logger log = Logger.getLogger(NetworkTrainer.class);

  private final static Map<Integer, List<TournamentResult>> tournamentYearResultsMap = new HashMap<>();
  private static List<TournamentResult> getTournamentResults(final Integer tournamentYear) {
    return tournamentYearResultsMap.computeIfAbsent(tournamentYear, k-> new ArrayList<>());
  }

  private final static Map<Integer, Map<String, SeasonData>> tournamentYearSeasonDataMap = new HashMap<>();
  private static Map<String, SeasonData> getTeamSeasonData(final Integer tournamentYear) {
    return tournamentYearSeasonDataMap.computeIfAbsent(tournamentYear, k -> new HashMap<>());
  }

  public static Optional<NetworkCandidate> trainNetwork(final Scanner scanner,
                                                        final SeasonDataDao seasonDataDao,
                                                        final TournamentResultDao tournamentResultDao,
                                                        final TournamentParticipantDao tournamentParticipantDao) throws IOException, InterruptedException {
    Optional<NetworkCandidate> ret = Optional.empty();
    NetworkParameters networkParameters = new NetworkParameters();
    scanner.reset();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      //
      // Fetch the network configuration parameters (reuse NetworkParameters so user doesn't have to re-enter everything each time)
      fetchNetworkParameters(bufferedReader, networkParameters);
      log.info("Network parameters acquired.");
      //
      // Get the season data for the teams in the training years
      List<Integer> yearsToTrain = networkParameters.getYearsToTrainAndEvaluateNetwork().get(0);
      log.info("Pulling training data...");
      RecordReaderDataSetIterator trainingDataIterator = createIterator(yearsToTrain, seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
      //
      // Get the season data for the teams in the evaluation years
      List<Integer> yearsToEvaluate = networkParameters.getYearsToTrainAndEvaluateNetwork().get(1);
      log.info("Pulling evaluation data...");
      RecordReaderDataSetIterator evaluationDataIterator = createIterator(yearsToEvaluate, seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
      //
      // Create the network definition
      log.info("Configuring network...");
      MultiLayerConfiguration configuration = configureNetwork(networkParameters);
      //
      // Train the network. If nothing goes wrong, return the trained network.
      log.info("Training network...");
      MultiLayerNetwork network = trainNetwork(configuration, networkParameters, trainingDataIterator, evaluationDataIterator);
      ret = keepOrDiscardNetwork(scanner, network, networkParameters);
      //
      // Ask if they want to train another network or quit
      System.out.println("Train another network (y/n)?");
      if (scanner.hasNext()) {
        String input = scanner.next();
        if (!input.equalsIgnoreCase("y")) {
          System.out.printf("Don't really understand '%s', quitting....%n", input);
          break;
        }
      }
    }
    return ret;
  }

  private static Optional<NetworkCandidate> keepOrDiscardNetwork(final Scanner scanner, final MultiLayerNetwork network, final NetworkParameters networkParameters) {
    Optional<NetworkCandidate> ret = Optional.empty();
    Boolean keep = null;
    while (keep == null) {
      System.out.println("Do you want to keep this network (y/n)?");
      if (scanner.hasNext()) {
        String input = scanner.next();
        if (input.equalsIgnoreCase("y") || input.equalsIgnoreCase("n")) {
          keep = (input.equalsIgnoreCase("y")) ? Boolean.TRUE : Boolean.FALSE;
        } else {
          System.out.printf("%s is not a valid choice. Please enter y or n.%n", input);
        }
      }
    }
    if (keep == Boolean.TRUE) {
      networkParameters.setWhenTrained(LocalDateTime.now());
      ret = Optional.of(new NetworkCandidate(networkParameters, network));
      log.info(String.format("Network %s retained.", network.toString()));
    } else {
      log.info(String.format("Network %s discarded.", network.toString()));
    }
    return ret;
  }

  private static void fetchNetworkParameters(final BufferedReader scanner, final NetworkParameters networkParameters) throws IOException {
    //
    // Get number of inputs
    networkParameters.setNumberOfInputs(23); // TODO: Get this from the user
    //
    // Get number of outputs
    networkParameters.setNumberOfOutputs(7); // TODO: Get this from the user
    //
    // Fetch years to train and evaluate the network
    scanYearsToTrainAndEvaluateNetwork(scanner, networkParameters);
    //
    // Get the network layout in HL1xHL2xHL3x...HLn format, where HL1 is the first hidden layer, HLn is the nth.
    String networkLayout = scanNetworkLayout(scanner, networkParameters.getNetworkLayout());
    networkParameters.setNetworkLayout(networkLayout);
    //
    // Get the activation function
    Activation activation = scanActivationFunction(scanner, networkParameters.getActivationFunction());
    networkParameters.setActivationFunction(activation);
    //
    // Get the Loss function
    LossFunctions.LossFunction lossFunction = scanLossFunction(scanner, networkParameters.getLossFunction());
    networkParameters.setLossFunction(lossFunction);
    //
    // Get the number of epochs
    Integer numberOfEpochs = scanNumberOfEpochs(scanner, networkParameters.getNumberOfEpochs());
    networkParameters.setNumberOfEpochs(numberOfEpochs);
    //
    // Get the WeightInit function
    WeightInit weightInit = scanWeightInitFunction(scanner, networkParameters.getWeightInit());
    networkParameters.setWeightInit(weightInit);
    //
    // Get the weight updater function
    IUpdater updater = scanUpdaterFunction(scanner, networkParameters.getUpdater());
    networkParameters.setUpdater(updater);
    //
    // Get the selected Elements
    List<DataElementMenuChoice> dataElementMenuChoices = scanDataElementChoice(scanner, networkParameters.getSelectedElements());
    networkParameters.setSelectedElements(dataElementMenuChoices);
  }

  private static RecordReaderDataSetIterator createIterator(final List<Integer> yearsForTraining,
                                                            final SeasonDataDao seasonDataDao,
                                                            final TournamentResultDao tournamentResultDao,
                                                            final TournamentParticipantDao tournamentParticipantDao,
                                                            final NetworkParameters networkParameters) throws IOException, InterruptedException {
    StringWriter stringWriter = new StringWriter();
    CSVWriter csvWriter = new CSVWriter(stringWriter);
    Map<String, SeasonData> teamNameSeasonDataMap = new HashMap<>(); // Optimization - do not create data for the same team twice
    for (Integer year : yearsForTraining) {
      List<TournamentResult> tournamentResults = pullTournamentResults(year, tournamentResultDao);
      for (TournamentResult tournamentResult : tournamentResults) {
        String winningTeamName = tournamentResult.getWinningTeamName();
        SeasonData seasonDataWinning = teamNameSeasonDataMap.get(winningTeamName);
        if (seasonDataWinning == null) {
          seasonDataWinning = pullSeasonData(year, winningTeamName, seasonDataDao);
          teamNameSeasonDataMap.put(winningTeamName, seasonDataWinning);
          TournamentParticipant tournamentParticipant = tournamentParticipantDao.fetchByYear(winningTeamName, year);
          //
          // Transform the data, then write out the data
          List<Double> rowWinDouble = writeSeasonData(seasonDataWinning, tournamentParticipant.getNumberOfVictories().doubleValue(), true);
          String[] rowWinString = networkParameters.transformRow(rowWinDouble, true);
          csvWriter.writeNext(rowWinString);
//          log.debug(String.format("Winning Team: %s (%d), data: %s", winningTeamName, tournamentParticipant.getNumberOfVictories(), Arrays.toString(rowWinString)));
        }
        // Losing team
        String losingTeamName = tournamentResult.getLosingTeamName();
        SeasonData seasonDataLosing = teamNameSeasonDataMap.get(losingTeamName);
        if (seasonDataLosing == null) {
          seasonDataLosing = pullSeasonData(year, losingTeamName, seasonDataDao);
          teamNameSeasonDataMap.put(losingTeamName, seasonDataLosing);
          TournamentParticipant tournamentParticipant = tournamentParticipantDao.fetchByYear(losingTeamName, year);
          //
          // Transform the data, then write out the data
          List<Double> rowLossDouble = writeSeasonData(seasonDataLosing, tournamentParticipant.getNumberOfVictories().doubleValue(), true);
          String[] rowLossString = networkParameters.transformRow(rowLossDouble, true);
          csvWriter.writeNext(rowLossString);
//          log.debug(String.format("Losing Team: %s (%d), data: %s", losingTeamName, tournamentParticipant.getNumberOfVictories(), Arrays.toString(rowLossString)));
        }
      }
    }
    //
    // Get the underlying String and via dl4j's arcane class structure transform it into a DataSet and return it
    StringBuffer stringBuffer = stringWriter.getBuffer();
    RecordReader recordReader = new CSVRecordReader(0, ',');
    recordReader.initialize(new InputStreamInputSplit(new ByteArrayInputStream(CharStreams.toString(new StringReader(stringBuffer.toString())).getBytes())));
    return new RecordReaderDataSetIterator(recordReader, 1000, networkParameters.getNumberOfInputs(), networkParameters.getNumberOfOutputs());
  }

  private static MultiLayerNetwork trainNetwork(final MultiLayerConfiguration configuration,
                                                final NetworkParameters networkParameters,
                                                final RecordReaderDataSetIterator trainingDataIterator,
                                                final RecordReaderDataSetIterator evaluationDataIterator) {
    log.info("Training network...");
    trainingDataIterator.setCollectMetaData(true);
    DataSet trainingData = trainingDataIterator.next();
    DataSet evaluationData = evaluationDataIterator.next();
    normalizeTrainingData(trainingData, evaluationData);

    //
    // Fit the model
    int numberOfEpochs = networkParameters.getNumberOfEpochs();
    int printIterationIndex = numberOfEpochs / 10;
    MultiLayerNetwork model = new MultiLayerNetwork(configuration);
    model.init();
    model.setListeners(new ScoreIterationListener(printIterationIndex));
    for (int epoch = 0; epoch < networkParameters.getNumberOfEpochs(); epoch++) {
      model.fit(trainingData);
    }

    // Evaluate the model on the test set
    Evaluation eval = new Evaluation(7);
    INDArray output = model.output(evaluationData.getFeatures());
    eval.eval(evaluationData.getLabels(), output, evaluationData.getExampleMetaData(RecordMetaData.class)); // Note we are passing in the test set metadata here
    networkParameters.setNetworkAccuracy(eval.accuracy());
    log.info(String.format("Evaluator stats: %s", eval.stats()));
    log.info(String.format("Network accuracy: %f%%", eval.accuracy() * 100.0));
    log.info("Training network...DONE");
    return model;
  }

  private static void normalizeTrainingData(final DataSet trainingData, final DataSet testData) {
    log.info("Normalizing data...");
    DataNormalization normalizer = new NormalizerStandardize();
//    DataNormalization normalizer = new NormalizerMinMaxScaler();
//    DataNormalization normalizer = new NormalizerMinMaxScaler(-1, 1);
    normalizer.fit(trainingData);           // Collect the statistics (mean/stdev) from the training data. This does not modify the input data
    normalizer.transform(trainingData);     // Apply normalization to the training data
    double[][] trainingDataFeatures = trainingData.getFeatures().toDoubleMatrix();
    for (double[] feature : trainingDataFeatures) {
      log.debug(String.format("normalizeTrainingData(): Training Data:\n%s", Arrays.toString(feature)));
    }
    normalizer.transform(testData);         // Apply normalization to the test data. This is using statistics calculated from the *training* set
    double[][] testDataFeatures = testData.getFeatures().toDoubleMatrix();
    for (double[] feature : trainingDataFeatures) {
      log.debug(String.format("normalizeTrainingData(): Test Data:\n%s", Arrays.toString(feature)));
    }
    log.info("Normalizing data...DONE");
  }

  private static MultiLayerConfiguration configureNetwork(final NetworkParameters networkParameters) {
    log.info("Configuring network...");
    NeuralNetConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
      .seed(6)
      .activation(networkParameters.getActivationFunction())
      .weightInit(networkParameters.getWeightInit())
      .updater(networkParameters.getUpdater())
      .l2(0.00001);
    NeuralNetConfiguration.ListBuilder listBuilder = builder.list();
    int[] networkLayout = Networks.parseNetworkStructure(networkParameters.getNetworkLayout());
    for (int index = 0; index < networkLayout.length - 2; index++) {
      listBuilder.layer(addHiddenLayer(networkLayout[index], networkLayout[index + 1]));
    }
    // Build the final layer and call build to create the MultiLayerConfiguration and return it
    MultiLayerConfiguration ret = listBuilder.layer(new OutputLayer.Builder(networkParameters.getLossFunction())
      .activation(Activation.SOFTMAX).nIn(networkLayout[networkLayout.length - 2]).nOut(networkLayout[networkLayout.length - 1]).build())
      .build();
    log.info("Configuring network...DONE");
    return ret;
  }

  private static DenseLayer addHiddenLayer(final int numberOfInputs, final int numberOfOutputs) {
    return new DenseLayer.Builder()
      .nIn(numberOfInputs)
      .nOut(numberOfOutputs)
      .build();
  }

  /**
   * Get a list of the years for which the network is to be trained, and a list
   * of the years for which the network is to be evaluated, and return them both
   * in a wrapper list.
   * <p>
   * The first element in the wrapper list is a the list of training years.
   */
  private static void scanYearsToTrainAndEvaluateNetwork(final BufferedReader scanner, final NetworkParameters networkParameters) {
    System.out.println("This is the Train Network Menu.");
    System.out.println("Enter year(s) for training (enter multiple years separated by commas)");

    List<Integer> yearsToTrain = networkParameters.getYearsToTrainAndEvaluateNetwork().get(0);
    scanListOfValidYears(scanner, yearsToTrain);
    log.info("Training data year(s): " + StringUtils.join(yearsToTrain, ','));

    System.out.println("Enter year(s) for evaluation/validation (enter multiple years separated by commas)");
    List<Integer> yearsToEvaluate = networkParameters.getYearsToTrainAndEvaluateNetwork().get(1);
    scanListOfValidYears(scanner, yearsToEvaluate);
    log.info("Evaluation data year(s): " + StringUtils.join(yearsToEvaluate, ','));
  }

  private static void scanListOfValidYears(final BufferedReader scanner, final List<Integer> listOfYears) {
    try {
      String userInput = null;
      while (StringUtils.isEmpty(userInput)) {
        if (!listOfYears.isEmpty()) {
          System.out.println("Press enter to keep your previous selection of " + StringUtils.join(listOfYears, ','));
        }
        // Get user input - a comma-separated list of years
        userInput = scanner.readLine();
        if (StringUtils.isNotEmpty(userInput)) {
          listOfYears.clear();
          // Break it up into tokens
          StringTokenizer strtok = new StringTokenizer(userInput, ",");
          while (strtok.hasMoreTokens()) {
            Integer year = Integer.valueOf(strtok.nextToken().trim());
            // Validate year
            NetworkUtils.validateYear(year);
            listOfYears.add(year);
          }
        } else if (!listOfYears.isEmpty()){
          break;
        } else {
          System.out.println("==> "); // Prompt
        }
      }
    } catch (Exception e) {
      String message = String.format("Error getting years for training: %s", e.getLocalizedMessage());
      log.error(message);
      throw new RuntimeException(message, e);
    }
  }

  private static String scanNetworkLayout(final BufferedReader scanner, final String networkLayout) throws IOException {
    String ret = (StringUtils.isNotEmpty(networkLayout)) ? networkLayout : "23x43x87x37x23x7";
    String userInput = null;
    while (StringUtils.isEmpty(userInput)) {
      System.out.println("Enter the network structure (for example, " + ret + "):");
      if (StringUtils.isNotEmpty(networkLayout)) {
        System.out.println("Press enter to keep your previous choice of " + networkLayout + " or enter a new one below:");
      }
      userInput = scanner.readLine();
      if (StringUtils.isNotEmpty(userInput)) {
        Networks.parseNetworkStructure(userInput);
      } else if (StringUtils.isNotEmpty(networkLayout)){
        ret = networkLayout;
        System.out.println("Using your previous choice of " + networkLayout + "....");
        break;
      } else {
        System.out.println("==> ");
      }
    }
    log.info("Network layout: " + ret);
    return ret;
  }

  private static Activation scanActivationFunction(final BufferedReader scanner, final Activation activation) throws IOException {
    Activation ret = (activation == null) ? Activation.TANH : activation;
    Optional<ActivationFunctionMenuChoice> menuChoice = ActivationFunctionMenuChoice.menu(scanner, activation);
    if (menuChoice.isPresent()) {
      ret = menuChoice.get().getActivation();
    }
    return ret;
  }

  private static IUpdater scanUpdaterFunction(final BufferedReader scanner, final IUpdater updater) throws IOException {
    IUpdater ret = (updater == null) ? new Nesterovs() : updater;
    Optional<UpdaterMenuChoice> menuChoice = UpdaterMenuChoice.menu(scanner,updater);
    if (menuChoice.isPresent()) {
      ret = menuChoice.get().getUpdater();
    }
    return ret;
  }

  private static WeightInit scanWeightInitFunction(final BufferedReader scanner, final WeightInit weightInit) throws IOException {
    WeightInit ret = (weightInit == null) ? WeightInit.XAVIER : weightInit;
    Optional<WeightInitMenuChoice> menuChoice = WeightInitMenuChoice.menu(scanner, weightInit);
    if (menuChoice.isPresent()) {
      ret = menuChoice.get().getWeightInit();
    }
    return ret;
  }

  private static Integer scanNumberOfEpochs(final BufferedReader scanner, final Integer numberOfEpochs) throws IOException {
    Integer ret = numberOfEpochs;
    String userInput = null;
    while (StringUtils.isEmpty(userInput)) {
      System.out.println("Enter the number of epochs: ");
      if (numberOfEpochs != null) {
        System.out.println("Press enter to use the number of epochs you used last time: " + numberOfEpochs);
      }
      userInput = scanner.readLine();
      if (StringUtils.isEmpty(userInput)) {
        break;
      } else if (StringUtils.isNumeric(userInput)) {
        ret = Integer.parseInt(userInput);
      } else {
        String message = String.format("Not a valid value for number of epochs: %s", userInput);
        log.warn(message);
        ret = null;
      }
    }
    return ret;
  }

  private static LossFunctions.LossFunction scanLossFunction(final BufferedReader scanner, final LossFunctions.LossFunction lossFunction) {
    // TODO: Ask the user for this
    if (lossFunction == null) {
      return LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD;
    } else {
      return lossFunction;
    }
//    return LossFunctions.LossFunction.HINGE;
  }

  private static List<DataElementMenuChoice> scanDataElementChoice(final BufferedReader scanner, final List<DataElementMenuChoice> choices) {
    // TODO: Ask the user for this
    if (choices == null) {
      return Arrays.asList(DataElementMenuChoice.values());
    } else {
      return choices;
    }
  }

  private static SeasonData pullSeasonData(final Integer year, final String teamName, final SeasonDataDao seasonDataDao) {
    return getTeamSeasonData(year).computeIfAbsent(teamName, k-> {
      log.debug(String.format("Reading %s %d season data from DB...", teamName, year));
      return seasonDataDao.fetchByYearAndTeamName(year, teamName);
    });
  }

  private static List<TournamentResult> pullTournamentResults(final Integer year, final TournamentResultDao tournamentResultDao) {
    List<TournamentResult> ret = getTournamentResults(year);
    if (ret.isEmpty()) {
      log.debug(String.format("Reading %d tournament results from DB...", year));
      ret.addAll(tournamentResultDao.fetchAllByYear(year));
    }
    return ret;
  }

  public static List<Double> writeSeasonData(final SeasonData seasonData) {
    return writeSeasonData(seasonData, null, false);
  }

  public static List<Double> writeSeasonData(final SeasonData seasonData,
                                             final Double resultClass,
                                             final boolean isTraining) {
    List<Double> ret = new ArrayList<>();
    // Offense
    ret.add(seasonData.getAvgPointsPerGame().doubleValue());
    ret.add(seasonData.getScoringMarginPerGame().doubleValue());
    ret.add(seasonData.getNumFgAttemptsPerGame().doubleValue());
    ret.add(seasonData.getFgPercentage().doubleValue());
    ret.add(seasonData.getNum3pPerGame().doubleValue());
    ret.add(seasonData.getNum3pAttemptsPerGame().doubleValue());
    ret.add(seasonData.getT3pPercentage().doubleValue());
    ret.add(seasonData.getNumFtAttemptsPerGame().doubleValue());
    ret.add(seasonData.getFtPercentage().doubleValue());
    ret.add(seasonData.getReboundMargin().doubleValue());
    ret.add(seasonData.getAssistsPerGame().doubleValue());
    ret.add(seasonData.getAtoRatio().doubleValue());
    // Defense
    ret.add(seasonData.getAvgOpponentPointsPerGame().doubleValue());
    ret.add(seasonData.getNumOppFgAttemptsPerGame().doubleValue());
    ret.add(seasonData.getOppFgPercentage().doubleValue());
    ret.add(seasonData.getNumOpp3pAttemptsPerGame().doubleValue());
    ret.add(seasonData.getOpp3pPercentage().doubleValue());
    ret.add(seasonData.getBlocksPerGame().doubleValue());
    ret.add(seasonData.getStealsPerGame().doubleValue());
    ret.add(seasonData.getOppTurnoversPerGame().doubleValue());
    // Errors
    ret.add(seasonData.getTurnoversPerGame().doubleValue());
    ret.add(seasonData.getFoulsPerGame().doubleValue());
    ret.add(seasonData.getNumDq().doubleValue());
    // Class - the number of tournament victories
    if (isTraining) {
      ret.add(resultClass);
    }
    return ret;
  }


}
