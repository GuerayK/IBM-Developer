package com.makotojava.ncaabb.generation;

import com.makotojava.ncaabb.springconfig.ApplicationConfig;
import org.datavec.api.records.metadata.RecordMetaData;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class NcaaBbNetTrainer {

  private static final Logger log = LoggerFactory.getLogger(NcaaBbNetTrainer.class);

  private static final int BATCH_SIZE = Integer.MAX_VALUE; // The mini-batch size for training

  public NcaaBbNetTrainer(final ApplicationContext applicationContext) {

  }

  public static void main(final String[] args) {
    NcaaBbNetTrainer trainer = new NcaaBbNetTrainer(new AnnotationConfigApplicationContext(ApplicationConfig.class));
  }

  private void go() {
    //    String trainingDataFileName = "/Users/sperry/home/development/projects/ScienceFair-2021/TrainingData/SeasonData_2010_2011_2012_2013_2014_2015_2016_2017_2018.csv";
    String trainingDataFileName = "/Users/sperry/home/development/projects/ScienceFair-2021/TrainingData/SeasonData_2010_2011_2012_2013_2014_2015_2016_2017.csv";
//    String trainingDataFileName = "/Users/sperry/home/development/projects/ScienceFair-2021/TrainingData/SeasonData_2018.csv";
//    String testFileName = "/Users/sperry/home/development/projects/ScienceFair-2021/TrainingData/SeasonData_2019.csv";
    String testFileName = "/Users/sperry/home/development/projects/ScienceFair-2021/TrainingData/SeasonData_2018.csv";
    String modelFilename = "/Users/sperry/home/development/projects/ScienceFair-2021/Networks/NcaaBB-" + System.currentTimeMillis() + ".net";
    int numberOfInputs = 46; // TODO: Get from ApplicationConfig
    int numberOfOutputs = 2; // TODO: Get from ApplicationConfig
    try (RecordReader trainingIterator = new CSVRecordReader(0, ',')) {
      trainingIterator.initialize(new FileSplit(new File(trainingDataFileName)));
      RecordReaderDataSetIterator trainingRecordIterator = new RecordReaderDataSetIterator(trainingIterator, BATCH_SIZE, numberOfInputs, numberOfOutputs);
      trainingRecordIterator.setCollectMetaData(true);  // Instruct the iterator to collect metadata, and store it in the DataSet objects
      DataSet trainingData = trainingRecordIterator.next();
      trainingData.shuffle(123);

      RecordReader testRecordReader = new CSVRecordReader(0, ',');
      testRecordReader.initialize(new FileSplit(new File(testFileName)));
      RecordReaderDataSetIterator testIterator = new RecordReaderDataSetIterator(testRecordReader, BATCH_SIZE, numberOfInputs, numberOfOutputs);
      testIterator.setCollectMetaData(true);  // Instruct the iterator to collect metadata, and store it in the DataSet objects
      DataSet testData = testIterator.next();
      DataSetLossCalculator dataSetLossCalculator = new DataSetLossCalculator(trainingRecordIterator, false);

      // Let's view the example metadata in the training and test sets:
      List<RecordMetaData> trainMetaData = trainingData.getExampleMetaData(RecordMetaData.class);
      List<RecordMetaData> testMetaData = testData.getExampleMetaData(RecordMetaData.class);

      // Let's show specifically which examples are in the training and test sets, using the collected metadata
      logTrainingDataInfo(trainMetaData, testMetaData);

      // Normalize data as per basic CSV example
      normalizeTrainingData(trainingData, testData);

      long seed = 6;

      // Compute the network configuration
      MultiLayerConfiguration conf = computeNetworkConfiguration(numberOfInputs, numberOfOutputs, seed);

      // Fit the model
      int printIterationIndex = 1000;
      MultiLayerNetwork model = new MultiLayerNetwork(conf);
      model.init();
      model.setListeners(new ScoreIterationListener(printIterationIndex));

      for (int i = 0; i < 5000; i++) {
        model.fit(trainingData);
        if (i % printIterationIndex == 0) {
          System.out.printf("Model score: %s%n", dataSetLossCalculator.calculateScore(model));
        }
      }

      // Evaluate the model on the test set
      Evaluation eval = new Evaluation(2);
      INDArray output = model.output(testData.getFeatures());
      eval.eval(testData.getLabels(), output, testMetaData); // Note we are passing in the test set metadata here
      System.out.println(eval.stats());

      log.info("Save model...");
      ModelSerializer.writeModel(model, modelFilename, true);

    } catch (IOException | InterruptedException e) {
      String errorMessage = String.format("Error occurred while processing file '%s' %s", trainingDataFileName, e.getLocalizedMessage());
      log.error(errorMessage, e);
      throw new RuntimeException(errorMessage, e);
    }

  }

  private static void normalizeTrainingData(final DataSet trainingData, final DataSet testData) {
    DataNormalization normalizer = new NormalizerStandardize();
    normalizer.fit(trainingData);           // Collect the statistics (mean/stdev) from the training data. This does not modify the input data
    normalizer.transform(trainingData);     // Apply normalization to the training data
    normalizer.transform(testData);         // Apply normalization to the test data. This is using statistics calculated from the *training* set
  }

  private static void logTrainingDataInfo(final List<RecordMetaData> trainMetaData, final List<RecordMetaData> testMetaData) {
    System.out.println("  +++++ Training Set Examples MetaData +++++");
    String format = "%-20s\t%s";
    for (RecordMetaData recordMetaData : trainMetaData) {
      log.info(String.format(format, recordMetaData.getLocation(), recordMetaData.getURI()));
      // Also available: recordMetaData.getReaderClass()
    }
    System.out.println("\n\n  +++++ Test Set Examples MetaData +++++");
    for (RecordMetaData recordMetaData : testMetaData) {
      log.info(recordMetaData.getLocation());
    }
  }

  private static MultiLayerConfiguration computeNetworkConfiguration(final int numberOfInputs, final int numberOfOutputs, final long seed) {
    System.out.println("Build model....92x147x46x23x11");
    int layer1 = 69;
//    int layer2 = 92;
//    int layer3 = 69;
    int layer4 = 46;
    int layer5 = 23;
    Activation activation = Activation.TANH;
    MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
      .seed(seed)
      .activation(activation)
      .weightInit(WeightInit.XAVIER)
      .updater(new Nesterovs(0.1))
      .l2(0.0001)
      .list()
      .layer(new DenseLayer.Builder().nIn(numberOfInputs).nOut(layer1).build()) // 1
      .layer(new DenseLayer.Builder().nIn(layer1).nOut(layer1).build())
//      .layer(new DenseLayer.Builder().nIn(layer1).nOut(layer1).build())
//      .layer(new DenseLayer.Builder().nIn(layer1).nOut(layer2).build()) // 2
//      .layer(new DenseLayer.Builder().nIn(layer2).nOut(layer2).build())
//      .layer(new DenseLayer.Builder().nIn(layer2).nOut(layer2).build())
//      .layer(new DenseLayer.Builder().nIn(layer2).nOut(layer3).build()) // 3
//      .layer(new DenseLayer.Builder().nIn(layer3).nOut(layer3).build())
//      .layer(new DenseLayer.Builder().nIn(layer3).nOut(layer3).build())
//      .layer(new DenseLayer.Builder().nIn(layer3).nOut(layer4).build()) // 4
      .layer(new DenseLayer.Builder().nIn(layer1).nOut(layer4).build())
//      .layer(new DenseLayer.Builder().nIn(layer4).nOut(layer4).build())
      .layer(new DenseLayer.Builder().nIn(layer4).nOut(layer5).build()) // 5
//      .layer(new DenseLayer.Builder().nIn(layer5).nOut(layer5).build())
//      .layer(new DenseLayer.Builder().nIn(layer5).nOut(layer5).build())
      .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
        .activation(Activation.SOFTMAX).nIn(layer5).nOut(numberOfOutputs).build())
      .build();
    return conf;
  }

}
