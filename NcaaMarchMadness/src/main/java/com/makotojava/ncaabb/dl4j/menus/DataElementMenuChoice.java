package com.makotojava.ncaabb.dl4j.menus;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum DataElementMenuChoice {
  // Offense
  // The ordinal corresponds to the index in the input data where that element is located.
  // See NetworkParameters.transformRow()
  AVERAGE_POINTS_PER_GAME(1, 0),
  SCORING_MARGIN_PER_GAME(2, 1),
  NUMBER_FIELD_GOAL_ATTEMPTS_PER_GAME(3, 2),
  FIELD_GOAL_PERCENTAGE(4, 3),
  NUMBER_THREE_POINTERS_PER_GAME(5, 4),
  NUMBER_THREE_POINT_ATTEMPTS_PER_GAME(6, 5),
  THREE_POINT_PERCENTAGE(7, 6),
  NUMBER_FREE_THROW_ATTEMPTS_PER_GAME(8, 7),
  FREE_THROW_PERCENTAGE(9, 8),
  REBOUND_MARGIN(10, 9),
  ASSISTS_PER_GAME(11, 10),
  ASSIST_TO_TURNOVER_RATIO(12, 11),
  // Defense
  AVERAGE_OPPONENT_POINTS_PER_GAME(13, 12),
  NUMBER_OPPONENT_FIELD_GOAL_ATTEMPTS_PER_GAME(14, 13),
  OPPONENT_FIELD_GOAL_PERCENTAGE(15, 14),
  NUMBER_OPPONENT_THREE_POINT_ATTEMPTS_PER_GAME(16, 15),
  OPPONENT_THREE_POINT_PERCENTAGE(17, 16),
  BLOCKS_PER_GAME(18, 17),
  STEALS_PER_GAME(19, 18),
  OPPONENT_TURNOVERS_PER_GAME(20, 19),
  // Errors
  TURNOVERS_PER_GAME(21, 20),
  FOULS_PER_GAME(22, 21),
  NUMBER_DISQUALIFICATIONS(23, 22);

  private static final Logger log = Logger.getLogger(DataElementMenuChoice.class);
  private final int ordinal;
  private final int elementIndex;

  DataElementMenuChoice(final int ordinal, final int elementIndex) {
    this.ordinal = ordinal;
    this.elementIndex = elementIndex;
  }

  private static Optional<DataElementMenuChoice> from(final int choice) {
    Optional<DataElementMenuChoice> ret = Optional.empty();
    for (DataElementMenuChoice menuChoice : values()) {
      if (menuChoice.getOrdinal() == choice) {
        ret = Optional.of(menuChoice);
        break;
      }
    }
    return ret;
  }

  public static List<DataElementMenuChoice> menu(final BufferedReader scanner, final List<DataElementMenuChoice> selectedElements) throws IOException {
    String thing = "data element";
    List<DataElementMenuChoice> ret = (selectedElements == null) ? new ArrayList<>() : selectedElements;
    String userInput;
    // Loop until the user finishes
    while (true) {
      if (!ret.isEmpty()) {
        System.out.println("******* CURRENT CHOICES *******");
        Collections.sort(ret);
        displayMenuItems(ret.toArray(new DataElementMenuChoice[0]));
        System.out.println("(To use these choices just press enter)");
        System.out.printf("To remove a choice enter -NUMBER (for example, -%d removes %s from your selections)%n", ret.get(0).getOrdinal(), ret.get(0).name());
      }
      System.out.printf("Enter the number of the %s you want to select (enter 0 to quit): %n", thing);
      displayMenuItems(filter(values(), ret));
      userInput = scanner.readLine();
      if (StringUtils.isEmpty(userInput)) {
        // User wants their previous choices, we're done here
        break;
      } else if (isNumeric(userInput)) {
        int userChoice = Integer.parseInt(userInput);
        if (userChoice == 0) {
          // Zero means quit, this is just for consistency with other menus, hitting enter works too
          break;
        }
        processUserChoice(ret, userChoice);
      } else {
        String message = String.format("Not a valid value: %s", userInput);
        log.warn(message);
      }
    }
    return ret;
  }

  private static boolean isNumeric(final String userInput) {
    return StringUtils.isNumeric(userInput) ||
      userInput.startsWith("-") && StringUtils.isNumeric(StringUtils.substring(userInput, 1))
      ;
  }

  private static DataElementMenuChoice[] filter(final DataElementMenuChoice[] values,
                                                final List<DataElementMenuChoice> selectedElements) {
    List<DataElementMenuChoice> ret = Arrays.asList(values);
    if (selectedElements != null && !selectedElements.isEmpty()) {
      ret = Arrays.stream(values).filter(dataElementMenuChoice -> !selectedElements.contains(dataElementMenuChoice))
        .collect(Collectors.toList());
    }
    return ret.toArray(new DataElementMenuChoice[0]);
  }

  private static void processUserChoice(final List<DataElementMenuChoice> dataElementMenuChoices, int userChoice) {
    boolean removeChoice = false;
    // If the choice is < 0, then the user wants to remove that choice
    if (userChoice < 0) {
      userChoice *= -1;
      removeChoice = true;
    }
    Optional<DataElementMenuChoice> menuChoice = from(userChoice);
    if (menuChoice.isPresent()) {
      DataElementMenuChoice dataElementMenuChoice = menuChoice.get();
      if (removeChoice) {
        log.debug("You chose to remove element: " + dataElementMenuChoice);
        dataElementMenuChoices.remove(dataElementMenuChoice);
      } else {
        log.debug("You chose element: " + menuChoice.get());
        dataElementMenuChoices.add(dataElementMenuChoice);
      }
    } else {
      System.out.printf("%d is not a valid choice, please choose from the list.%n", userChoice);
    }
  }

  private static void displayMenuItems(DataElementMenuChoice[] choices) {
    int numberOfColumns = 3;
    int numberOfRows = choices.length / numberOfColumns + choices.length % numberOfColumns;
    for (int row = 0; row < numberOfRows; row++) {
      for (int column = 0; column < numberOfColumns; column++) {
        int index = row + (column * numberOfRows);
        if (index < choices.length) {
          DataElementMenuChoice choice = choices[index];
          System.out.printf("%2d - %48s", choice.getOrdinal(), choice.name());
        }
        System.out.print("\t\t");
      }
      System.out.print('\n');
    }
  }

  public int getElementIndex() {
    return elementIndex;
  }

  private int getOrdinal() {
    return ordinal;
  }

}
