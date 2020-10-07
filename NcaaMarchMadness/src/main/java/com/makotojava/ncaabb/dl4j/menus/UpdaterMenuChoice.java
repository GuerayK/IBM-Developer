package com.makotojava.ncaabb.dl4j.menus;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.nd4j.linalg.learning.config.AMSGrad;
import org.nd4j.linalg.learning.config.AdaDelta;
import org.nd4j.linalg.learning.config.AdaGrad;
import org.nd4j.linalg.learning.config.AdaMax;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.learning.config.Nadam;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.learning.config.NoOp;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.learning.config.Sgd;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

public enum UpdaterMenuChoice {
  ADA_DELTA(new AdaDelta(), 1),
  ADA_GRAD(new AdaGrad(), 2),
  ADAM(new Adam(), 3),
  ADA_MAX(new AdaMax(), 4),
  AMS_GRAD(new AMSGrad(), 5),
  NADAM(new Nadam(), 6),
  NESTEROVS(new Nesterovs(), 7),
  NOOP(new NoOp(), 8),
  RMS_PROP(new RmsProp(), 9),
  SGD(new Sgd(), 10);

  private static final Logger log = Logger.getLogger(UpdaterMenuChoice.class);
  private final IUpdater updater;
  private final int ordinal;

  UpdaterMenuChoice(final IUpdater updater, final int ordinal) {
    this.updater = updater;
    this.ordinal = ordinal;
  }

  private static Optional<UpdaterMenuChoice> from(final int choice) {
    Optional<UpdaterMenuChoice> ret = Optional.empty();
    for (UpdaterMenuChoice menuChoice : values()) {
      if (menuChoice.getOrdinal() == choice) {
        ret = Optional.of(menuChoice);
        break;
      }
    }
    return ret;
  }

  private static Optional<UpdaterMenuChoice> from(final IUpdater updater) {
    Optional<UpdaterMenuChoice> ret = Optional.empty();
    for (UpdaterMenuChoice menuChoice : values()) {
      if (menuChoice.getUpdater().equals(updater)) {
        ret = Optional.of(menuChoice);
        break;
      }
    }
    return ret;
  }

  public static Optional<UpdaterMenuChoice> menu(final BufferedReader scanner, final IUpdater updater) throws IOException {
    String thing = "weight updater function";
    Optional<UpdaterMenuChoice> ret = from(updater);
    String userInput = null;
    while (StringUtils.isEmpty(userInput)) {
      System.out.printf("Enter the number of the %s you want to use: %n", thing);
      if (updater != null) {
        System.out.printf("Press enter to use the %s you used last time: %s%n", thing, updater);
      }
      displayMenuItems(values());
      userInput = scanner.readLine();
      if (StringUtils.isEmpty(userInput)) {
        // User wants their previous choice
        break;
      } else if (StringUtils.isNumeric(userInput)) {
        int userChoice = Integer.parseInt(userInput);
        Optional<UpdaterMenuChoice> menuChoice = from(userChoice);
        if (menuChoice.isPresent()) {
          log.debug("You chose updater function: " + menuChoice.get());
          ret = menuChoice;
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

  private static void displayMenuItems(UpdaterMenuChoice[] choices) {
    int numberOfColumns = 3;
    int numberOfRows = choices.length / numberOfColumns + choices.length % numberOfColumns;
    for (int row = 0; row < numberOfRows; row++) {
      for (int column = 0; column < numberOfColumns; column++) {
        int index = row+(column*numberOfRows);
        if (index < choices.length) {
          UpdaterMenuChoice choice = choices[index];
          System.out.printf("%2d - %10s", choice.getOrdinal(), choice.name());
        }
        System.out.print("\t\t");
      }
      System.out.print('\n');
    }
  }

  public IUpdater getUpdater() {
    return updater;
  }

  public int getOrdinal() {
    return ordinal;
  }

}
