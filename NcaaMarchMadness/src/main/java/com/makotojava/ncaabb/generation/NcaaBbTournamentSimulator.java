package com.makotojava.ncaabb.generation;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.InputStreamInputSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.shade.guava.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

public class NcaaBbTournamentSimulator {
  private static final Logger log = LoggerFactory.getLogger(NcaaBbTournamentSimulator.class);

  public static void main(final String[] args) throws IOException {
    // Load the model
//    String modelFileName = "/Users/sperry/home/development/projects/ScienceFair-2021/Networks/NcaaBB-1601432785597.net";
    String modelFileName = "/Users/sperry/home/development/projects/ScienceFair-2021/Networks/NcaaBB-1601438918868.net";

    // Tournament data file
//    String tournamentDataFile = "/Users/sperry/home/development/projects/ScienceFair-2021/Simulation/2019_UCF_vs_VCU.csv";
//    String tournamentDataFile = "/Users/sperry/home/development/projects/ScienceFair-2021/Simulation/2019_VCU_vs_UCF.csv";
//    String tournamentDataFile = "/Users/sperry/home/development/projects/ScienceFair-2021/Simulation/2019_Ole Miss_vs_Liberty.csv";
//    String tournamentDataFile = "/Users/sperry/home/development/projects/ScienceFair-2021/Simulation/2019_North Carolina.csv";
    String tournamentDataFile = "/Users/sperry/home/development/projects/ScienceFair-2021/TrainingData/TournamentData-2018.csv";

    MultiLayerNetwork network = MultiLayerNetwork.load(new File(modelFileName), true);

    try (RecordReader tournamentReader = new CSVRecordReader(0, ',')) {
      InputStreamInputSplit split = new InputStreamInputSplit(fetchInputStream());
      tournamentReader.initialize(split);
      RecordReaderDataSetIterator tournamentIterator = new RecordReaderDataSetIterator(tournamentReader, 1);
      INDArray output = network.output(tournamentIterator);
      log.info("Output: {}", output.toStringFull());
    } catch (InterruptedException e) {
      String errorMessage = String.format("Error occurred while processing file '%s' %s", tournamentDataFile, e.getLocalizedMessage());
      log.error(errorMessage, e);
      throw new RuntimeException(errorMessage, e);
    }
  }

  private static InputStream fetchInputStream() throws IOException {
    StringBuilder stringBuilder = new StringBuilder("87.05882,16.17647,61.52941,0.5043,11.41176,28.67647,0.39795,17.61765,0.77129,2.88236,16.67647,1.6108,70.88235,60.20588,0.43429,21.35294,0.32782,3.97059,6.67647,13.11765,10.35294,15.85294,8.0,67.35294,3.0,55.35294,0.42614,7.73529,22.11765,0.34973,17.20588,0.72308,3.20588,12.20588,1.00242,64.35294,52.79412,0.42841,20.35294,0.34682,2.94118,6.29412,12.97059,12.17647,16.38235,6.0");
    stringBuilder.append('\n');
    stringBuilder.append("67.35294,3.0,55.35294,0.42614,7.73529,22.11765,0.34973,17.20588,0.72308,3.20588,12.20588,1.00242,64.35294,52.79412,0.42841,20.35294,0.34682,2.94118,6.29412,12.97059,12.17647,16.38235,6.0,87.05882,16.17647,61.52941,0.5043,11.41176,28.67647,0.39795,17.61765,0.77129,2.88236,16.67647,1.6108,70.88235,60.20588,0.43429,21.35294,0.32782,3.97059,6.67647,13.11765,10.35294,15.85294,8.0");
    StringReader stringReader = new StringReader(stringBuilder.toString());

    return new ByteArrayInputStream(CharStreams.toString(stringReader).getBytes());
  }
}
