package com.makotojava.ncaabb.dl4j;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;

/**
 * A network that has been trained and the user has decided to keep.
 * That doesn't mean the network will make the final cut, but it
 * is a candidate for persistence. Hence the name.
 */
public class NetworkCandidate {
  private final NetworkParameters networkParameters;
  private final MultiLayerNetwork multiLayerNetwork;

  public NetworkCandidate(final NetworkParameters networkParameters,
                          final MultiLayerNetwork multiLayerNetwork) {

    this.networkParameters = networkParameters;
    this.multiLayerNetwork = multiLayerNetwork;
  }

  public NetworkParameters getNetworkParameters() {
    return networkParameters;
  }

  public MultiLayerNetwork getMultiLayerNetwork() {
    return multiLayerNetwork;
  }
}
