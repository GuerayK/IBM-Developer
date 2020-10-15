package com.makotojava.ncaabb.dl4j;

import com.makotojava.ncaabb.dao.SeasonDataDao;
import com.makotojava.ncaabb.dao.TournamentParticipantDao;
import com.makotojava.ncaabb.dao.TournamentResultDao;
import com.makotojava.ncaabb.dl4j.menus.MainMenuChoice;
import com.makotojava.ncaabb.dl4j.model.NetworkCandidate;
import com.makotojava.ncaabb.dl4j.model.NetworkParameters;
import com.makotojava.ncaabb.springconfig.ApplicationConfig;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainMenu {
  private static final Logger log = LoggerFactory.getLogger(MainMenu.class);

  private final List<NetworkCandidate> unsavedNetworks = new ArrayList<>();

  private static ApplicationContext applicationContext;

  public static ApplicationContext getApplicationContext() { return applicationContext; }

  private final SeasonDataDao seasonDataDao;
  private final TournamentResultDao tournamentResultDao;
  private final TournamentParticipantDao tournamentParticipantDao;

  private NetworkParameters networkParameters;

  public MainMenu(final ApplicationContext applicationContext) {
    this.seasonDataDao = applicationContext.getBean(SeasonDataDao.class);
    this.tournamentResultDao = applicationContext.getBean(TournamentResultDao.class);
    this.tournamentParticipantDao = applicationContext.getBean(TournamentParticipantDao.class);
  }

  public static void main(final String[] args) {
    applicationContext = new AnnotationConfigApplicationContext(ApplicationConfig.class);
    MainMenu mainMenu = new MainMenu(applicationContext);
    //
    // No arguments - prompt the user for EVERYTHING
    BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
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
  private static MainMenuChoice displayMainMenu(final BufferedReader scanner) throws IOException {
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
    String line = scanner.readLine();
    if (StringUtils.isNotEmpty(line)) {
      //
      // Get the menu choice from the number entered by the user or UNKNOWN
      ret = MainMenuChoice.from(Integer.parseInt(StringUtils.strip(line)));
    } else {
      log.error("{}: {}", ret.getDisplayValue(), line);
    }
    return ret;
  }

  private MainMenuChoice processMainMenuChoice(final BufferedReader scanner, final MainMenuChoice menuChoice) throws IOException, InterruptedException {
    MainMenuChoice ret = menuChoice;
    switch (menuChoice) {
      case QUIT:
        if (unsavedNetworks.size() > 0) {
          while (true) {
            System.out.println("You have unsaved networks. Are you sure you want to quit (y/n)?");
            String input = StringUtils.strip(scanner.readLine());
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
        Optional<NetworkCandidate> trainedNetwork = NetworkTrainer.trainNetwork(networkParameters, seasonDataDao, tournamentResultDao, tournamentParticipantDao);
        trainedNetwork.ifPresent(networkCandidate -> {
          unsavedNetworks.add(networkCandidate);
          networkParameters = networkCandidate.getNetworkParameters();
        });
        break;
      case EVALUATE_NETWORK:
        NetworkEvaluator.go(scanner, unsavedNetworks);
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
      case LIST_SAVED_NETWORKS:
        NetworkUtils.displayNetworks();
        break;
    }
    return ret;
  }

}
