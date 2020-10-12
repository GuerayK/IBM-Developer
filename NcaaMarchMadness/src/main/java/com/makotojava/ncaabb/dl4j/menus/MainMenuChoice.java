package com.makotojava.ncaabb.dl4j.menus;

public enum MainMenuChoice {
  UNKNOWN("That is not a choice", -1),
  TRAIN_NETWORK("Train a new neural network", 1),
  PERSIST_UNSAVED_NETWORKS("Persist unsaved neural network(s)", 2),
  LIST_SAVED_NETWORKS("List saved networks", 3),
  RUN_NETWORK("Run a trained neural network", 4),
  EVALUATE_NETWORK("Evaluate a trained neural network", 6),
  QUIT("Quit the program", 0);

  private String displayValue;
  private int ordinalValue;

  MainMenuChoice(final String displayValue, final int ordinalValue) {
    this.displayValue = displayValue;
    this.ordinalValue = ordinalValue;
  }

  public String getDisplayValue() {
    return this.displayValue;
  }

  public int getOrdinalValue() {
    return ordinalValue;
  }

  public static MainMenuChoice from(final int ordinalValueCandiate) {
    MainMenuChoice ret = MainMenuChoice.UNKNOWN;
    for (MainMenuChoice mainMenuChoice : values()) {
      if (mainMenuChoice.getOrdinalValue() == ordinalValueCandiate) {
        ret = mainMenuChoice;
        break;
      }
    }
    return ret;
  }

  public boolean isSuitableForDisplay() {
    return this != MainMenuChoice.UNKNOWN;
  }
}
