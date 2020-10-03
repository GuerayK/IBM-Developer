package com.makotojava.ncaabb.dl4j;

import com.makotojava.ncaabb.dao.SeasonDataDao;
import com.makotojava.ncaabb.dao.TournamentResultDao;
import com.makotojava.ncaabb.generation.Networks;
import com.makotojava.ncaabb.model.SeasonData;
import com.makotojava.ncaabb.model.TournamentResult;
import com.makotojava.ncaabb.util.NetworkUtils;
import com.opencsv.CSVWriter;
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
import org.nd4j.weightinit.WeightInit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.StringTokenizer;

public class NetworkTrainer {

  private static final Logger log = Logger.getLogger(NetworkTrainer.class);

  public static Optional<NetworkCandidate> trainNetwork(final Scanner scanner,
                                                         final SeasonDataDao seasonDataDao,
                                                         final TournamentResultDao tournamentResultDao) throws IOException, InterruptedException {
    Optional<NetworkCandidate> ret = Optional.empty();
    //
    // Fetch the network configuration parameters
    NetworkParameters networkParameters = fetchNetworkParameters(scanner);
    //
    // Get the season data for the teams in the training years
    List<Integer> yearsToTrain = networkParameters.getYearsToTrainAndEvaluateNetwork().get(0);
    RecordReaderDataSetIterator trainingDataIterator = createIterator(yearsToTrain, seasonDataDao, tournamentResultDao, networkParameters);
    //
    // Get the season data for the teams in the evaluation years
    List<Integer> yearsToEvaluate = networkParameters.getYearsToTrainAndEvaluateNetwork().get(1);
    RecordReaderDataSetIterator evaluationDataIterator = createIterator(yearsToEvaluate, seasonDataDao, tournamentResultDao, networkParameters);
    //
    // Create the network definition
    MultiLayerConfiguration configuration = configureNetwork(networkParameters);
    //
    // Train the network. If nothing goes wrong, return the trained network.
    MultiLayerNetwork network = trainNetwork(configuration, networkParameters, trainingDataIterator, evaluationDataIterator);
    ret = keepOrDiscardNetwork(scanner, network, networkParameters);
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

  private static NetworkParameters fetchNetworkParameters(final Scanner scanner) {
    NetworkParameters ret = new NetworkParameters();
    //
    // Get number of inputs
    ret.setNumberOfInputs(46); // TODO: Get this from the user
    //
    // Get number of outputs
    ret.setNumberOfOutputs(2); // TODO: Get this from the user
    //
    // Fetch years to train and evaluate the network
    List<List<Integer>> yearsToTrainAndEvaluateNetwork = scanYearsToTrainAndEvaluateNetwork(scanner);
    ret.setYearsToTrainAndEvaluateNetwork(yearsToTrainAndEvaluateNetwork);
    //
    // Get the network layout in HL1xHL2xHL3x...HLn format, where HL1 is the first hidden layer, HLn is the nth.
    String networkLayout = scanNetworkLayout(scanner);
    ret.setNetworkLayout(networkLayout);
    //
    // Get the activation function
    Activation activation = scanActivationFunction(scanner);
    ret.setActivationFunction(activation);
    //
    // Get the Loss function
    LossFunctions.LossFunction lossFunction = scanLossFunction(scanner);
    ret.setLossFunction(lossFunction);
    //
    // Get the number of epochs
    Integer numberOfEpochs = scanNumberOfEpochs(scanner);
    ret.setNumberOfEpochs(numberOfEpochs);
    //
    // Get the WeightInit function
    WeightInit weightInit = scanWeightInitFunction(scanner);
    ret.setWeightInit(weightInit);
    //
    // Get the weight updater function
    IUpdater updater = scanUpdaterFunction(scanner);
    ret.setUpdater(updater);
    //
    // Get the selected Elements
    List<DataElementMenuChoice> dataElementMenuChoices = scanDataElementChoice(scanner);
    ret.setSelectedElements(dataElementMenuChoices);
    return ret;
  }

  private static RecordReaderDataSetIterator createIterator(final List<Integer> yearsForTraining,
                                                            final SeasonDataDao seasonDataDao,
                                                            final TournamentResultDao tournamentResultDao,
                                                            final NetworkParameters networkParameters) throws IOException, InterruptedException {
    StringWriter stringWriter = new StringWriter();
    CSVWriter csvWriter = new CSVWriter(stringWriter);
    for (Integer year : yearsForTraining) {
      List<TournamentResult> tournamentResults = pullTournamentResults(year, tournamentResultDao);
      for (TournamentResult tournamentResult : tournamentResults) {
        String winningTeamName = tournamentResult.getWinningTeamName();
        String losingTeamName = tournamentResult.getLosingTeamName();
        SeasonData seasonDataWinning = pullSeasonData(year, winningTeamName, seasonDataDao);
        SeasonData seasonDataLosing = pullSeasonData(year, losingTeamName, seasonDataDao);
        // Attempt to eliminate positional bias
        List<Double> rowWinDouble = writeSeasonData(seasonDataWinning, seasonDataLosing, 1.0);
        List<Double> rowLossDouble = writeSeasonData(seasonDataLosing, seasonDataWinning, 0.0);
        //
        // Transform the data, then write out the data
        String[] rowWinString = networkParameters.transformRow(rowWinDouble);
        csvWriter.writeNext(rowWinString);
        String[] rowLossString = networkParameters.transformRow(rowLossDouble);
        csvWriter.writeNext(rowLossString);
      }
    }
    //
    // Get the underlying String and via dl4j's arcane class structure transform it into a DataSet and return it
    StringBuffer stringBuffer = stringWriter.getBuffer();
    RecordReader recordReader = new CSVRecordReader(0, ',');
    recordReader.initialize(new InputStreamInputSplit(new ByteArrayInputStream(CharStreams.toString(new StringReader(stringBuffer.toString())).getBytes())));
    RecordReaderDataSetIterator iterator = new RecordReaderDataSetIterator(recordReader, 1000, networkParameters.getNumberOfInputs(), networkParameters.getNumberOfOutputs());
    return iterator;
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
    Evaluation eval = new Evaluation(2);
    INDArray output = model.output(evaluationData.getFeatures());
    eval.eval(evaluationData.getLabels(), output, evaluationData.getExampleMetaData(RecordMetaData.class)); // Note we are passing in the test set metadata here
    networkParameters.setNetworkAccuracy(eval.accuracy());
    log.info(String.format("Evaluator stats: %s", eval.stats()));
    log.info(String.format("Network accuracy: %f%%", eval.accuracy()*100.0));
    log.info("Training network...DONE");
    return model;
  }

  private static void normalizeTrainingData(final DataSet trainingData, final DataSet testData) {
    log.info("Normalizing data...");
    DataNormalization normalizer = new NormalizerStandardize();
    normalizer.fit(trainingData);           // Collect the statistics (mean/stdev) from the training data. This does not modify the input data
    normalizer.transform(trainingData);     // Apply normalization to the training data
    normalizer.transform(testData);         // Apply normalization to the test data. This is using statistics calculated from the *training* set
    log.info("Normalizing data...DONE");
  }

  private static MultiLayerConfiguration configureNetwork(final NetworkParameters networkParameters) {
    log.info("Configuring network...");
    NeuralNetConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
      .seed(3)
      .activation(networkParameters.getActivationFunction())
      .weightInit(org.deeplearning4j.nn.weights.WeightInit.XAVIER)
      .updater(networkParameters.getUpdater())
      .l2(0.0001);
    NeuralNetConfiguration.ListBuilder listBuilder = builder.list();
    int[] networkLayout = Networks.parseNetworkStructure(networkParameters.getNetworkLayout());
    for (int index = 0; index < networkLayout.length - 2; index++) {
      listBuilder.layer(addHiddenLayer(networkLayout[index], networkLayout[index + 1]));
    }
    // Build the final layer and call build to create the MultiLayerConfiguration and return it
    MultiLayerConfiguration ret = listBuilder.layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
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
  private static List<List<Integer>> scanYearsToTrainAndEvaluateNetwork(final Scanner scanner) {
    List<List<Integer>> ret = new ArrayList<>();
    System.out.println("This is the Train Network Menu.");
    System.out.println("Enter year(s) for training (enter multiple years separated by commas)");
    System.out.println("==> ");
    Optional<List<Integer>> yearsForTraining = fetchListOfValidYears(scanner);
    if (yearsForTraining.isPresent()) {
      ret.add(yearsForTraining.get());
      System.out.println("Enter year(s) for evaluation/validation (enter multiple years separated by commas)");
      System.out.println("==> ");
      Optional<List<Integer>> yearsForEvaluation = fetchListOfValidYears(scanner);
      yearsForEvaluation.ifPresent(ret::add);
    }
    return ret;
  }

  private static String scanNetworkLayout(final Scanner scanner) {
    String ret = "46x92x147x46x23x11x2";
    System.out.println("Enter the network structure (for example, 46x92x147x46x23x11x2):");
    if (scanner.hasNext()) {
      ret = scanner.next();
      Networks.parseNetworkStructure(ret);
    } else {
      String message = String.format("Something went wrong, returning the default: %s", ret);
      log.error(message);
    }
    return ret;
  }

  private static IUpdater scanUpdaterFunction(final Scanner scanner) {
    // TODO: Ask the user for this
    return new Nesterovs(0.1);
  }

  private static WeightInit scanWeightInitFunction(final Scanner scanner) {
    // TODO: Ask the user for this
    return WeightInit.XAVIER;
  }

  private static Integer scanNumberOfEpochs(final Scanner scanner) {
    int ret;
    System.out.println("Enter the number of epochs: ");
    if (scanner.hasNextInt()) {
      ret = scanner.nextInt();
    } else {
      String message = String.format("Not a valid value for number of epochs: %s", scanner.next());
      log.error(message);
      throw new RuntimeException(message);
    }
    return ret;
  }

  private static LossFunctions.LossFunction scanLossFunction(final Scanner scanner) {
    // TODO: Ask the user for this
    return LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD;
  }

  private static Activation scanActivationFunction(final Scanner scanner) {
    // TODO: Ask the user for this
    return Activation.TANH;
  }

  private static List<DataElementMenuChoice> scanDataElementChoice(final Scanner scanner) {
    // TODO: Ask the user for this
    return Arrays.asList(DataElementMenuChoice.values());
  }

  private static Optional<List<Integer>> fetchListOfValidYears(final Scanner scanner) {
    Optional<List<Integer>> ret = Optional.empty();
    final List<Integer> listOfYears = new ArrayList<>();
    try {
      if (scanner.hasNext()) {
        // Get user input - a comma-separated list of years
        String userInput = scanner.next();
        // Break it up into tokens
        StringTokenizer strtok = new StringTokenizer(userInput, ",");
        while (strtok.hasMoreTokens()) {
          Integer year = Integer.valueOf(strtok.nextToken().trim());
          // Validate year
          NetworkUtils.validateYear(year);
          listOfYears.add(year);
        }
        // If one or more years were specified and are valid, add to return value
        if (!listOfYears.isEmpty()) {
          ret = Optional.of(listOfYears);
        }
      } else {
        // This should be rare, but needs to be covered.
        String message = "Not sure what to do. Scanner is empty. Returning...";
        log.error(message);
        throw new RuntimeException(message);
      }
    } catch (RuntimeException e) {
      String message = String.format("Error getting years for training: %s", e.getLocalizedMessage());
      log.error(message);
      throw new RuntimeException(message, e);
    }
    return ret;
  }

  private static SeasonData pullSeasonData(final Integer year, final String teamName, final SeasonDataDao seasonDataDao) {
    return seasonDataDao.fetchByYearAndTeamName(year, teamName);
  }

  private static List<TournamentResult> pullTournamentResults(final Integer year, final TournamentResultDao tournamentResultDao) {
    return tournamentResultDao.fetchAllByYear(year);
  }

  public static List<Double> writeSeasonData(final SeasonData home,
                                             final SeasonData away,
                                             final Double result) {
    List<Double> ret = new ArrayList<>();
    // Offense
    ret.add(home.getAvgPointsPerGame().doubleValue());
    ret.add(away.getAvgPointsPerGame().doubleValue());
    ret.add(home.getScoringMarginPerGame().doubleValue());
    ret.add(away.getScoringMarginPerGame().doubleValue());
    ret.add(home.getNumFgAttemptsPerGame().doubleValue());
    ret.add(away.getNumFgAttemptsPerGame().doubleValue());
    ret.add(home.getFgPercentage().doubleValue());
    ret.add(away.getFgPercentage().doubleValue());
    ret.add(home.getNum3pPerGame().doubleValue());
    ret.add(away.getNum3pPerGame().doubleValue());
    ret.add(home.getNum3pAttemptsPerGame().doubleValue());
    ret.add(away.getNum3pAttemptsPerGame().doubleValue());
    ret.add(home.getT3pPercentage().doubleValue());
    ret.add(away.getT3pPercentage().doubleValue());
    ret.add(home.getNumFtAttemptsPerGame().doubleValue());
    ret.add(away.getNumFtAttemptsPerGame().doubleValue());
    ret.add(home.getFtPercentage().doubleValue());
    ret.add(away.getFtPercentage().doubleValue());
    ret.add(home.getReboundMargin().doubleValue());
    ret.add(away.getReboundMargin().doubleValue());
    ret.add(home.getAssistsPerGame().doubleValue());
    ret.add(away.getAssistsPerGame().doubleValue());
    ret.add(home.getAtoRatio().doubleValue());
    ret.add(away.getAtoRatio().doubleValue());
    // Defense
    ret.add(home.getAvgOpponentPointsPerGame().doubleValue());
    ret.add(away.getAvgOpponentPointsPerGame().doubleValue());
    ret.add(home.getNumOppFgAttemptsPerGame().doubleValue());
    ret.add(away.getNumOppFgAttemptsPerGame().doubleValue());
    ret.add(home.getOppFgPercentage().doubleValue());
    ret.add(away.getOppFgPercentage().doubleValue());
    ret.add(home.getNumOpp3pAttemptsPerGame().doubleValue());
    ret.add(away.getNumOpp3pAttemptsPerGame().doubleValue());
    ret.add(home.getOpp3pPercentage().doubleValue());
    ret.add(away.getOpp3pPercentage().doubleValue());
    ret.add(home.getBlocksPerGame().doubleValue());
    ret.add(away.getBlocksPerGame().doubleValue());
    ret.add(home.getStealsPerGame().doubleValue());
    ret.add(away.getStealsPerGame().doubleValue());
    ret.add(home.getOppTurnoversPerGame().doubleValue());
    ret.add(away.getOppTurnoversPerGame().doubleValue());
    // Errors
    ret.add(home.getTurnoversPerGame().doubleValue());
    ret.add(away.getTurnoversPerGame().doubleValue());
    ret.add(home.getFoulsPerGame().doubleValue());
    ret.add(away.getFoulsPerGame().doubleValue());
    ret.add(home.getNumDq().doubleValue());
    ret.add(away.getNumDq().doubleValue());
    // Class - Win (1.0) or Lose (0.0) - only if not null
    if (result != null) {
      ret.add(result);
    }
    return ret;
  }


}
