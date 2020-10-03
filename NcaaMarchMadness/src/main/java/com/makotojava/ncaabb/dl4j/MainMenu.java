package com.makotojava.ncaabb.dl4j;

import com.makotojava.ncaabb.dao.SeasonDataDao;
import com.makotojava.ncaabb.dao.TournamentResultDao;
import com.makotojava.ncaabb.springconfig.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class MainMenu {
  private static final Logger log = LoggerFactory.getLogger(MainMenu.class);

  private final List<NetworkCandidate> unsavedNetworks = new ArrayList<>();

  private final SeasonDataDao seasonDataDao;
  private final TournamentResultDao tournamentResultDao;

  public MainMenu(final ApplicationContext applicationContext) {
    this.seasonDataDao = applicationContext.getBean(SeasonDataDao.class);
    this.tournamentResultDao = applicationContext.getBean(TournamentResultDao.class);
  }

  public static void main(final String[] args) {
    MainMenu mainMenu = new MainMenu(new AnnotationConfigApplicationContext(ApplicationConfig.class));
    //
    // No arguments - prompt the user for EVERYTHING
    Scanner scanner = new Scanner(System.in);
    scanner.useDelimiter("\n");
    //
    // Display main menu. When it terminates, so does program.
    MainMenuChoice menuChoice = MainMenuChoice.UNKNOWN;
    while (menuChoice != MainMenuChoice.QUIT) {
      try {
        //
        // Display the main menu, let the user make their choice
        menuChoice = displayMainMenu(scanner);
        //
        // Process the user's choice
        menuChoice = mainMenu.processMainMenuChoice(scanner, menuChoice);
      } catch (Throwable t) {
        //
        // Something bad happened, but we soldier on...
        log.error("Bad things happened, man: {}", t.getLocalizedMessage(), t);
      }
    }
    //
    // Bye now.
    log.info("Program terminated. Goodbye.");
  }

  /**
   * Display the main menu. Javadoc. Check.
   */
  private static MainMenuChoice displayMainMenu(final Scanner scanner) {
    MainMenuChoice ret = MainMenuChoice.UNKNOWN;
    //
    // Show the options and camp out until they decide to quit
    // Use System.out.println() because it just looks better
    System.out.println("Welcome to NCAA Basketball Net.");
    System.out.println("This is the main menu. What do you want to do?");
    for (MainMenuChoice menuItem : MainMenuChoice.values()) {
      if (menuItem.isSuitableForDisplay()) {
        System.out.printf("%d - %s%n", menuItem.getOrdinalValue(), menuItem.getDisplayValue());
      }
    }
    System.out.println("==> ");
    if (scanner.hasNextByte()) {
      //
      // Get the menu choice from the number entered by the user or UNKNOWN
      ret = MainMenuChoice.from(scanner.nextByte());
    } else {
      log.error("{}: {}", ret.getDisplayValue(), scanner.next());
    }
    return ret;
  }

  private MainMenuChoice processMainMenuChoice(final Scanner scanner, final MainMenuChoice menuChoice) throws IOException, InterruptedException {
    MainMenuChoice ret = menuChoice;
    switch (menuChoice) {
      case QUIT:
        if (unsavedNetworks.size() > 0) {
          while (true) {
            System.out.println("You have unsaved networks. Are you sure you want to quit (y/n)?");
            String input = scanner.next().trim();
            if (input.equalsIgnoreCase("y")) {
              break;
            } else if (input.equalsIgnoreCase("n")) {
              // Set the return value to UNKNOWN so the main menu displays again
              ret = MainMenuChoice.UNKNOWN;
              break;
            }
          }
        }
        break;
      case TRAIN_NETWORK:
        Optional<NetworkCandidate> trainedNetwork = NetworkTrainer.trainNetwork(scanner, seasonDataDao, tournamentResultDao);
        trainedNetwork.ifPresent(unsavedNetworks::add);
        break;
      case EVALUATE_NETWORK:
        System.out.println("This is where you will evaluate a network someday!");
        break;
      case RUN_NETWORK:
        NetworkRunner.go(scanner, unsavedNetworks);
        break;
      case PERSIST_UNSAVED_NETWORKS:
        if (unsavedNetworks.isEmpty()) {
          System.out.println("No networks to save.");
        } else {
          NetworkPersister.persistNetworks(scanner, unsavedNetworks);
        }
        break;
    }
    return ret;
  }

}
