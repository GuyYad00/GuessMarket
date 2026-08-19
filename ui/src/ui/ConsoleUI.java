package ui;

import engine.api.Engine;
import engine.dto.BuyResultDTO;
import engine.dto.CloseResultDTO;
import engine.dto.EventDTO;
import engine.dto.OptionStateDTO;
import engine.dto.TradeDTO;
import engine.dto.TradeStateDTO;
import engine.exception.EngineOperationException;
import engine.exception.InvalidFileException;

import java.util.List;
import java.util.Scanner;

/**
 * The console user interface - the only place in the system that prints
 * to the screen and reads input from the user. Talks to the engine
 * exclusively through the {@link Engine} interface and its DTOs.
 */
public class ConsoleUI {

    private final Engine engine;
    private final Scanner scanner;

    public ConsoleUI(Engine engine) {
        this.engine = engine;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        System.out.println("Welcome to Guess Market!");
        boolean exit = false;
        while (!exit) {
            printMenu();
            int choice = readMenuChoice();
            try {
                switch (choice) {
                    case 1 -> loadFileCommand();
                    case 2 -> showEventsCommand();
                    case 3 -> showTradeStateCommand();
                    case 4 -> buySharesCommand();
                    case 5 -> closeEventCommand();
                    case 6 -> exit = true;
                    case 7 -> saveStateCommand();
                    case 8 -> loadStateCommand();
                    default -> System.out.println("Error: please choose a number between 1 and 8.");
                }
            } catch (InvalidFileException | EngineOperationException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        System.out.println("Thank you for using Guess Market. Goodbye!");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("--------------- Guess Market Menu ---------------");
        System.out.println("1. Load system details file");
        System.out.println("2. Show events");
        System.out.println("3. Show event trade status");
        System.out.println("4. Buy shares in an event");
        System.out.println("5. Close (resolve) an event");
        System.out.println("6. Exit");
        System.out.println("7. Save system state to file (bonus)");
        System.out.println("8. Load system state from file (bonus)");
        System.out.println("--------------------------------------------------");
        System.out.print("Please choose a command (1-8): ");
    }

    private int readMenuChoice() {
        String line = scanner.nextLine().trim();
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return -1; // handled as an unknown command by the menu loop
        }
    }

    // --- Command 1 ---

    private void loadFileCommand() {
        System.out.print("Please enter the full path of the XML file to load: ");
        String path = scanner.nextLine().trim();
        engine.loadFile(path);
        System.out.println("The file was found valid and loaded successfully into the system.");
    }

    // --- Command 2 ---

    private void showEventsCommand() {
        List<EventDTO> events = engine.getEvents();
        System.out.println();
        System.out.println("There are " + events.size() + " event(s) in the system:");
        for (EventDTO event : events) {
            printEventDetails(event);
        }
    }

    private void printEventDetails(EventDTO event) {
        System.out.println();
        System.out.println("Event number:      " + event.getId());
        System.out.println("Name:              " + event.getName());
        System.out.println("Description:       " + event.getDescription());
        System.out.println("Commission:        " + event.getCommissionPercentage() + "%");
        System.out.println("Commission method: " + event.getCommissionType());
        System.out.println("Options:           " + formatOptions(event.getOptionNames()));
        System.out.println("Status:            " + (event.isActive() ? "Active" : "Closed"));
    }

    private String formatOptions(List<String> optionNames) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < optionNames.size(); i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(i + 1).append(") ").append(optionNames.get(i));
        }
        return sb.toString();
    }

    // --- Command 3 ---

    private void showTradeStateCommand() {
        List<EventDTO> events = engine.getEvents();
        EventDTO chosen = chooseEvent(events, "Choose the event whose trade status you would like to see");
        if (chosen == null) {
            return;
        }
        printTradeState(engine.getTradeState(chosen.getId()));
    }

    private void printTradeState(TradeStateDTO state) {
        System.out.println();
        System.out.println("Trade status of event #" + state.getEventId() + " - '" + state.getEventName() + "':");
        printCurrentState(state);
        System.out.printf("Event account balance:      %.2f%n", state.getAccountBalance());
        System.out.printf("Total commission collected: %.2f%n", state.getTotalCommissionCollected());

        System.out.println("Trade history (newest first):");
        if (state.getTradesNewestFirst().isEmpty()) {
            System.out.println("  No trades were made in this event yet.");
        } else {
            for (TradeDTO trade : state.getTradesNewestFirst()) {
                System.out.printf("  Option: %-25s | Shares: %-8d | Price paid: %.2f%n",
                        trade.getOptionName(), trade.getShares(), trade.getTotalPaid());
            }
        }

        if (!state.isActive()) {
            System.out.println("This event is closed. Total shares bought per option:");
            for (OptionStateDTO option : state.getOptionStates()) {
                System.out.printf("  %-25s : %d share(s)%n", option.getName(), option.getTotalSharesBought());
            }
            System.out.println("The winning option is: " + state.getWinnerOptionName());
        }
    }

    private void printCurrentState(TradeStateDTO state) {
        System.out.println("Current state:");
        List<OptionStateDTO> options = state.getOptionStates();
        for (int i = 0; i < options.size(); i++) {
            OptionStateDTO option = options.get(i);
            System.out.printf("  %d) %-25s | Price: %.2f | Total shares bought: %d%n",
                    i + 1, option.getName(), option.getPrice(), option.getTotalSharesBought());
        }
    }

    // --- Command 4 ---

    private void buySharesCommand() {
        List<EventDTO> activeEvents = engine.getActiveEvents();
        EventDTO chosen = chooseEvent(activeEvents, "Choose the event you would like to participate in");
        if (chosen == null) {
            return;
        }

        TradeStateDTO state = engine.getTradeState(chosen.getId());
        System.out.println();
        printCurrentState(state);

        int optionNumber = readPositiveInt(
                "Choose the option you believe in (1-" + state.getOptionStates().size() + "): ",
                state.getOptionStates().size());
        if (optionNumber == -1) {
            return;
        }

        long shares = readPositiveLong("How many shares would you like to buy? ");
        if (shares == -1) {
            return;
        }

        BuyResultDTO result = engine.buyShares(chosen.getId(), optionNumber, shares);
        System.out.println();
        System.out.printf("Purchase completed: %d share(s) of '%s'.%n", result.getShares(), result.getOptionName());
        System.out.printf("Total paid: %.2f%n", result.getTotalPaid());
        System.out.printf("  Paid for the shares:     %.2f%n", result.getSharesCost());
        if (result.getCommissionPaid() > 0) {
            System.out.printf("  Paid as commission:      %.2f%n", result.getCommissionPaid());
        }
        printTradeState(result.getStateAfterPurchase());
    }

    // --- Command 5 ---

    private void closeEventCommand() {
        List<EventDTO> activeEvents = engine.getActiveEvents();
        EventDTO chosen = chooseEvent(activeEvents, "Choose the event you would like to close");
        if (chosen == null) {
            return;
        }

        TradeStateDTO state = engine.getTradeState(chosen.getId());
        printTradeState(state);

        int winnerNumber = readPositiveInt(
                "Choose the winning option this event ended with (1-" + state.getOptionStates().size() + "): ",
                state.getOptionStates().size());
        if (winnerNumber == -1) {
            return;
        }

        CloseResultDTO result = engine.closeEvent(chosen.getId(), winnerNumber);
        System.out.println();
        System.out.println("The event was closed. The winning option is: '" + result.getWinnerOptionName() + "'.");
        System.out.printf("Total paid to the winners: %.2f%n", result.getTotalPaidToWinners());
        if (result.getCommissionCollectedOnClose() > 0) {
            System.out.printf("Commission collected on close: %.2f%n", result.getCommissionCollectedOnClose());
        }
        printTradeState(result.getStateAfterClose());
    }

    // --- Bonus commands ---

    private void saveStateCommand() {
        System.out.print("Please enter the full path (including file name, without extension) to save to: ");
        String path = scanner.nextLine().trim();
        engine.saveState(path);
        System.out.println("The system state was saved successfully.");
    }

    private void loadStateCommand() {
        System.out.print("Please enter the full path (including file name, without extension) to load from: ");
        String path = scanner.nextLine().trim();
        engine.loadState(path);
        System.out.println("The system state was loaded successfully.");
    }

    // --- Shared input helpers ---

    /**
     * Presents the given events and lets the user choose one by its event number.
     * Returns null if the user aborted (empty input allows going back to the menu).
     */
    private EventDTO chooseEvent(List<EventDTO> events, String prompt) {
        if (events.isEmpty()) {
            System.out.println("There are no relevant events at the moment.");
            return null;
        }
        for (EventDTO event : events) {
            printEventDetails(event);
        }
        System.out.println();

        while (true) {
            System.out.print(prompt + " (event number, or press Enter to return to the menu): ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                return null;
            }
            try {
                int id = Integer.parseInt(line);
                for (EventDTO event : events) {
                    if (event.getId() == id) {
                        return event;
                    }
                }
                System.out.println("Error: there is no event with number " + id + " in the list above. Please try again.");
            } catch (NumberFormatException e) {
                System.out.println("Error: '" + line + "' is not a number. Please enter the event number.");
            }
        }
    }

    /** Reads an int between 1 and max. Returns -1 if the user aborted with an empty line. */
    private int readPositiveInt(String prompt, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                return -1;
            }
            try {
                int value = Integer.parseInt(line);
                if (value >= 1 && value <= max) {
                    return value;
                }
                System.out.println("Error: please choose a number between 1 and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Error: '" + line + "' is not a number. Please try again.");
            }
        }
    }

    /** Reads a positive whole number. Returns -1 if the user aborted with an empty line. */
    private long readPositiveLong(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                return -1;
            }
            try {
                long value = Long.parseLong(line);
                if (value > 0) {
                    return value;
                }
                System.out.println("Error: the amount must be a positive whole number.");
            } catch (NumberFormatException e) {
                System.out.println("Error: '" + line + "' is not a whole number. Please try again.");
            }
        }
    }
}
