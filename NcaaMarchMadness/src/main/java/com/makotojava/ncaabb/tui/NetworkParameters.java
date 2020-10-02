package com.makotojava.ncaabb.tui;

import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.weightinit.WeightInit;

import java.util.List;

public class NetworkParameters {
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
}
