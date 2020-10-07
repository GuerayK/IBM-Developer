package com.makotojava.ncaabb.dl4j.menus;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.nd4j.linalg.activations.Activation;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

public enum ActivationFunctionMenuChoice {

  TANH(Activation.TANH, 1),
  CUBE(Activation.CUBE, 2),
  ELU(Activation.ELU, 3),
  HARDSIGMOID(Activation.HARDSIGMOID, 4),
  HARDTANH(Activation.HARDTANH, 5),
  IDENTITY(Activation.IDENTITY, 6),
  LEAKYRELU(Activation.LEAKYRELU, 7),
  RATIONALTANH(Activation.RATIONALTANH, 8),
  RELU(Activation.RELU, 9),
  RELU6(Activation.RELU6, 10),
  RRELU(Activation.RRELU, 11),
  SIGMOID(Activation.SIGMOID, 12),
  SOFTMAX(Activation.SOFTMAX, 13),
  SOFTPLUS(Activation.SOFTPLUS, 14),
  SOFTSIGN(Activation.SOFTSIGN, 15),
  RECTIFIEDTANH(Activation.RECTIFIEDTANH, 16),
  SELU(Activation.SELU, 17),
  SWISH(Activation.SWISH, 18),
  THRESHOLDEDRELU(Activation.THRESHOLDEDRELU, 19),
  GELU(Activation.GELU, 20),
  MISH(Activation.MISH, 21);

  private static final Logger log = Logger.getLogger(ActivationFunctionMenuChoice.class);

  private final Activation activation;
  private final int ordinal;

  ActivationFunctionMenuChoice(final Activation activation, final int ordinal) {
    this.activation = activation;
    this.ordinal = ordinal;
  }

  private static Optional<ActivationFunctionMenuChoice> fromOrdinal(final int choice) {
    Optional<ActivationFunctionMenuChoice> ret = Optional.empty();
    for (ActivationFunctionMenuChoice activationFunctionMenuChoice : values()) {
      if (activationFunctionMenuChoice.getOrdinal() == choice) {
        ret = Optional.of(activationFunctionMenuChoice);
        break;
      }
    }
    return ret;
  }

  private static Optional<ActivationFunctionMenuChoice> fromActivation(final Activation activation) {
    Optional<ActivationFunctionMenuChoice> ret = Optional.empty();
    for (ActivationFunctionMenuChoice activationFunctionMenuChoice : values()) {
      if (activationFunctionMenuChoice.getActivation().equals(activation)) {
        ret = Optional.of(activationFunctionMenuChoice);
        break;
      }
    }
    return ret;
  }

  public static Optional<ActivationFunctionMenuChoice> menu(final BufferedReader scanner, final Activation activation) throws IOException {
    String thing = "activation function";
    Optional<ActivationFunctionMenuChoice> ret = fromActivation(activation);
    String userInput = null;
    while (StringUtils.isEmpty(userInput)) {
      System.out.printf("Enter the number of the %s you want to use: %n", thing);
      if (activation != null) {
        System.out.printf("Press enter to use the %s you used last time: %s%n", thing, activation);
      }
      displayMenuItems(values());
      userInput = scanner.readLine();
      if (StringUtils.isEmpty(userInput)) {
        // User wants their previous choice
        break;
      } else if (StringUtils.isNumeric(userInput)) {
        int userChoice = Integer.parseInt(userInput);
        Optional<ActivationFunctionMenuChoice> activationFunctionMenuChoice = fromOrdinal(userChoice);
        if (activationFunctionMenuChoice.isPresent()) {
          log.debug("You chose activation function: " + activationFunctionMenuChoice.get());
          ret = activationFunctionMenuChoice;
        } else {
          System.out.printf("%d is not a valid choice, please choose from the list.%n", userChoice);
          userInput = null;
        }
      } else {
        String message = String.format("Not a valid value: %s", userInput);
        log.warn(message);
        userInput = null;
      }
    }
    return ret;
  }

  private static void displayMenuItems(ActivationFunctionMenuChoice[] choices) {
    int numberOfColumns = 4;
    int numberOfRows = choices.length / numberOfColumns + choices.length % numberOfColumns;
    for (int row = 0; row < numberOfRows; row++) {
      for (int column = 0; column < numberOfColumns; column++) {
        int index = row+(column*numberOfRows);
        if (index < choices.length) {
          ActivationFunctionMenuChoice choice = choices[index];
          System.out.printf("%2d - %16s", choice.getOrdinal(), choice.name());
        }
        System.out.print("\t\t");
      }
      System.out.print('\n');
    }
  }

  public Activation getActivation() {
    return activation;
  }

  public int getOrdinal() {
    return ordinal;
  }
}
