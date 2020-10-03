package com.makotojava.ncaabb.dl4j;

import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.weightinit.WeightInit;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class NetworkParameters implements Serializable {
  private Integer numberOfInputs;
  private Integer numberOfOutputs;
  private List<List<Integer>> yearsToTrainAndEvaluateNetwork;
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

  public List<List<Integer>> getYearsToTrainAndEvaluateNetwork() {
    return yearsToTrainAndEvaluateNetwork;
  }

  public NetworkParameters setYearsToTrainAndEvaluateNetwork(final List<List<Integer>> yearsToTrainAndEvaluateNetwork) {
    this.yearsToTrainAndEvaluateNetwork = yearsToTrainAndEvaluateNetwork;
    return this;
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
    // Note: networkParameters is future-proofing, at some point the user can specify the specific fields they want from the data
    //
    // Pull the elements from the networkParameters.selectedElements only
    List<DataElementMenuChoice> selectedElements = getSelectedElements();
    //
    // New array contains all of the data, including the label (win/loss)
    // The selected elements describes the data once, but the List<Double> contains two sets of data: home and away
    String[] ret = new String[selectedElements.size() * 2 + 1];
    int index = 0;
    for (DataElementMenuChoice dataElementMenuChoice : selectedElements) {
      int elementIndex = dataElementMenuChoice.getElementIndex();
      ret[index++] = String.valueOf(data.get(elementIndex * 2)); // Home
      ret[index++] = String.valueOf(data.get(elementIndex * 2 + 1)); // Away
    }
    // Set the label (that is, the win/loss value AS AN INTEGER- THIS IS VERY IMPORTANT)
    ret[index] = String.valueOf(data.get(index).intValue());
    return ret;
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
}
