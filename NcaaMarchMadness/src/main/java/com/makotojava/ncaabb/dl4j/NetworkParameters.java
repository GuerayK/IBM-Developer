package com.makotojava.ncaabb.dl4j;

import com.makotojava.ncaabb.dl4j.menus.DataElementMenuChoice;
import org.apache.commons.lang3.StringUtils;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NetworkParameters implements Serializable {

  private static final long serialVersionUID = 4L;

  private Integer numberOfInputs;
  private Integer numberOfOutputs;
  private List<List<Integer>> yearsToTrainAndEvaluateNetwork = new ArrayList<>();
  private String networkLayout;
  private Activation activationFunction;
  private LossFunctions.LossFunction lossFunction;
  private Integer numberOfEpochs;
  private WeightInit weightInit;
  private IUpdater updater;
  private List<DataElementMenuChoice> selectedElements;
  private LocalDateTime whenTrained;
  private double networkAccuracy;
  private boolean networkSaved;

  public NetworkParameters copy() {
    NetworkParameters ret = new NetworkParameters()
      .setNetworkSaved(isNetworkSaved())
      .setActivationFunction(getActivationFunction())
      .setLossFunction(getLossFunction())
      .setNetworkAccuracy(getNetworkAccuracy())
      .setNetworkLayout(getNetworkLayout())
      .setNumberOfEpochs(getNumberOfEpochs())
      .setNumberOfInputs(getNumberOfInputs())
      .setNumberOfOutputs(getNumberOfOutputs())
      .setSelectedElements(getSelectedElements())
      .setUpdater(getUpdater())
      .setWeightInit(getWeightInit())
      .setWhenTrained(getWhenTrained());
      ret.getYearsToTrainAndEvaluateNetwork().get(0).addAll(getYearsToTrainAndEvaluateNetwork().get(0));
      ret.getYearsToTrainAndEvaluateNetwork().get(1).addAll(getYearsToTrainAndEvaluateNetwork().get(1));
      return ret;
  }

  private static String listToCsvString(final List<Integer> integerList) {
    String ret = StringUtils.EMPTY;
    if (integerList.size() > 0) {
      String[] years = new String[integerList.size()];
      for (int index = 0; index < years.length; index++) {
        years[index] = integerList.get(index).toString();
      }
      ret = String.join(",", years);
    }
    return ret;
  }

  public List<List<Integer>> getYearsToTrainAndEvaluateNetwork() {
    if (yearsToTrainAndEvaluateNetwork == null || yearsToTrainAndEvaluateNetwork.isEmpty()) {
      yearsToTrainAndEvaluateNetwork = new ArrayList<>();
      yearsToTrainAndEvaluateNetwork.add(new ArrayList<>()); // train
      yearsToTrainAndEvaluateNetwork.add(new ArrayList<>()); // evaluate
    }
    return yearsToTrainAndEvaluateNetwork;
  }

  public String getNetworkLayout() {
    return networkLayout;
  }

  public NetworkParameters setNetworkLayout(final String networkLayout) {
    this.networkLayout = networkLayout;
    return this;
  }

  public Activation getActivationFunction() {
    return activationFunction;
  }

  public NetworkParameters setActivationFunction(final Activation activationFunction) {
    this.activationFunction = activationFunction;
    return this;
  }

  public LossFunctions.LossFunction getLossFunction() {
    return lossFunction;
  }

  public NetworkParameters setLossFunction(final LossFunctions.LossFunction lossFunction) {
    this.lossFunction = lossFunction;
    return this;
  }

  public Integer getNumberOfEpochs() {
    return numberOfEpochs;
  }

  public NetworkParameters setNumberOfEpochs(final Integer numberOfEpochs) {
    this.numberOfEpochs = numberOfEpochs;
    return this;
  }

  public WeightInit getWeightInit() {
    return weightInit;
  }

  public NetworkParameters setWeightInit(final WeightInit weightInit) {
    this.weightInit = weightInit;
    return this;
  }

  public IUpdater getUpdater() {
    return updater;
  }

  public NetworkParameters setUpdater(final IUpdater updater) {
    this.updater = updater;
    return this;
  }

  public Integer getNumberOfInputs() {
    return numberOfInputs;
  }

  public NetworkParameters setNumberOfInputs(final Integer numberOfInputs) {
    this.numberOfInputs = numberOfInputs;
    return this;
  }

  public Integer getNumberOfOutputs() {
    return numberOfOutputs;
  }

  public NetworkParameters setNumberOfOutputs(final Integer numberOfOutputs) {
    this.numberOfOutputs = numberOfOutputs;
    return this;
  }

  public List<DataElementMenuChoice> getSelectedElements() {
    return selectedElements;
  }

  public NetworkParameters setSelectedElements(final List<DataElementMenuChoice> selectedElements) {
    this.selectedElements = selectedElements;
    return this;
  }

  public LocalDateTime getWhenTrained() {
    return whenTrained;
  }

  public NetworkParameters setWhenTrained(final LocalDateTime whenTrained) {
    this.whenTrained = whenTrained;
    return this;
  }

  public double getNetworkAccuracy() {
    return networkAccuracy;
  }

  public NetworkParameters setNetworkAccuracy(final double networkAccuracy) {
    this.networkAccuracy = networkAccuracy;
    return this;
  }

  public boolean isNetworkSaved() {
    return networkSaved;
  }

  public NetworkParameters setNetworkSaved(final boolean networkSaved) {
    this.networkSaved = networkSaved;
    return this;
  }

  /**
   * Transform the data from Double to Strings.
   * Pull only the data the user has selected, which is contained in the
   * NetworkParameters.selectedElements property.
   *
   * @param data The input data (Double objects)
   * @return String[] that contains only the data elements the user wants
   * to include to train, evaluate, and run the network.
   */
  public String[] transformRow(final List<Double> data) {
    return transformRow(data, false);
  }

  /**
   * Transform the data from Double to Strings.
   * Pull only the data the user has selected, which is contained in the
   * NetworkParameters.selectedElements property.
   *
   * @param data The input data (Double objects)
   * @return String[] that contains only the data elements the user wants
   * to include to train, evaluate, and run the network.
   * @param training true if the row of data is used for training, false if not.
   */
  public String[] transformRow(final List<Double> data, final boolean training) {
    // Note: networkParameters is future-proofing, at some point the user can specify the specific fields they want from the data
    //
    // Pull the elements from the networkParameters.selectedElements only
    List<DataElementMenuChoice> selectedElements = getSelectedElements();
    //
    // New array contains all of the data, including the label (win/loss)
    // The selected elements describes the data once, but the List<Double> contains two sets of data: home and away

    String[] ret = (training)
      // Only include a spot for a label if data used for training
      ? new String[selectedElements.size() + 1]
      // No label required
      : new String[selectedElements.size()];
    int index = 0;
    for (DataElementMenuChoice dataElementMenuChoice : selectedElements) {
      int elementIndex = dataElementMenuChoice.getElementIndex();
      ret[index++] = String.valueOf(data.get(elementIndex)); // Home
    }
    if (training) {
      // Set the label (that is, the win/loss value AS AN INTEGER- THIS IS VERY IMPORTANT)
      ret[index] = String.valueOf(data.get(index).intValue());
    }
    return ret;
  }

  /**
   * Returns true if the specified year was not used to train or validate
   * the network (otherwise that just wouldn't be fair), false otherwise.
   */
  public boolean isValidYearForTournamentPrediction(final Integer tournamentYear) {
    boolean ret = true;
    for (List<Integer> years : getYearsToTrainAndEvaluateNetwork()) {
      if (years.contains(tournamentYear)) {
        ret = false;
        break;
      }
    }
    return ret;
  }

  public String getTrainingYearsString() {
    return listToCsvString(getYearsToTrainAndEvaluateNetwork().get(0));
  }

  public String getEvaluationYearsString() {
    return listToCsvString(getYearsToTrainAndEvaluateNetwork().get(1));
  }

}
