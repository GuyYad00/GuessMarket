package engine.api;

import engine.core.CommissionType;
import engine.core.Event;
import engine.core.EventOption;
import engine.core.Market;
import engine.core.Trade;
import engine.dto.BuyResultDTO;
import engine.dto.CloseResultDTO;
import engine.dto.EventDTO;
import engine.dto.OptionStateDTO;
import engine.dto.TradeDTO;
import engine.dto.TradeStateDTO;
import engine.exception.EngineOperationException;
import engine.exception.InvalidFileException;
import engine.jaxb.GmEventJaxb;
import engine.jaxb.GuessMarketJaxb;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EngineImpl implements Engine {

    private static final int MAX_COMMISSION = 90;
    private static final int OPTIONS_PER_EVENT = 2;
    private static final String SAVE_FILE_EXTENSION = ".gm";

    private Market market; // null until a valid file is loaded

    // --- Command 1: file loading + validation ---

    @Override
    public void loadFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new InvalidFileException("The file path is empty. Please provide a full path to an XML file.");
        }
        String path = filePath.trim();

        File file = new File(path);
        if (!file.exists()) {
            throw new InvalidFileException("The file '" + path + "' does not exist. Please check the path and try again.");
        }
        if (file.isDirectory()) {
            throw new InvalidFileException("The path '" + path + "' is a directory, not an XML file.");
        }
        if (!path.toLowerCase().endsWith(".xml")) {
            throw new InvalidFileException("The file '" + path + "' is not an XML file (it must end with '.xml').");
        }

        GuessMarketJaxb root = unmarshal(file);
        Market newMarket = buildValidatedMarket(root);

        // Only now, after full validation, the new file overrides the previous one.
        market = newMarket;
    }

    private GuessMarketJaxb unmarshal(File file) {
        try {
            JAXBContext context = JAXBContext.newInstance(GuessMarketJaxb.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (GuessMarketJaxb) unmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            throw new InvalidFileException("The file could not be read as a valid Guess Market XML file. "
                    + "Make sure it matches the expected schema.", e);
        }
    }

    private Market buildValidatedMarket(GuessMarketJaxb root) {
        if (root == null || root.getEvents() == null || root.getEvents().getEventList().isEmpty()) {
            throw new InvalidFileException("The file does not contain any events (missing or empty 'GM-events' section).");
        }

        List<GmEventJaxb> jaxbEvents = root.getEvents().getEventList();
        validateUniqueIds(jaxbEvents);

        Market newMarket = new Market();
        for (GmEventJaxb jaxbEvent : jaxbEvents) {
            newMarket.addEvent(buildValidatedEvent(jaxbEvent));
        }
        return newMarket;
    }

    private void validateUniqueIds(List<GmEventJaxb> jaxbEvents) {
        Map<Integer, String> idToName = new HashMap<>();
        for (GmEventJaxb event : jaxbEvents) {
            String name = safeName(event);
            String existingName = idToName.get(event.getId());
            if (existingName != null) {
                throw new InvalidFileException("Duplicate event id " + event.getId()
                        + ": both '" + existingName + "' and '" + name
                        + "' use it. Every event must have a unique id.");
            }
            idToName.put(event.getId(), name);
        }
    }

    private Event buildValidatedEvent(GmEventJaxb jaxbEvent) {
        String name = safeName(jaxbEvent);

        if (jaxbEvent.getDescription() == null || jaxbEvent.getDescription().trim().isEmpty()) {
            throw new InvalidFileException("Event '" + name + "' (id " + jaxbEvent.getId() + ") has no description.");
        }

        if (jaxbEvent.getComision() == null) {
            throw new InvalidFileException("Event '" + name + "' (id " + jaxbEvent.getId() + ") has no commission definition.");
        }
        int commission = jaxbEvent.getComision().getPercentage();
        if (commission < 0 || commission > MAX_COMMISSION) {
            throw new InvalidFileException("Event '" + name + "' (id " + jaxbEvent.getId()
                    + ") has an illegal commission of " + commission
                    + ". The commission must be between 0 and " + MAX_COMMISSION + " (inclusive).");
        }

        CommissionType commissionType;
        try {
            commissionType = CommissionType.fromXmlValue(jaxbEvent.getComision().getType());
        } catch (IllegalArgumentException e) {
            throw new InvalidFileException("Event '" + name + "' (id " + jaxbEvent.getId()
                    + ") has an unknown commission type '" + jaxbEvent.getComision().getType()
                    + "'. Allowed values: 'on-close', 'on-purchase'.");
        }

        List<String> optionNames = extractOptionNames(jaxbEvent, name);

        if (jaxbEvent.getMethod() == null || jaxbEvent.getMethod().getLmsr() == null) {
            throw new InvalidFileException("Event '" + name + "' (id " + jaxbEvent.getId()
                    + ") has no LMSR trading method definition.");
        }
        int b = jaxbEvent.getMethod().getLmsr().getB();
        if (b <= 0) {
            throw new InvalidFileException("Event '" + name + "' (id " + jaxbEvent.getId()
                    + ") has an illegal liquidity value b=" + b + ". It must be a positive whole number.");
        }

        return new Event(jaxbEvent.getId(), name, jaxbEvent.getDescription().trim(),
                commission, commissionType, optionNames, b);
    }

    private List<String> extractOptionNames(GmEventJaxb jaxbEvent, String eventName) {
        if (jaxbEvent.getOptions() == null || jaxbEvent.getOptions().getOptionList().size() != OPTIONS_PER_EVENT) {
            int found = (jaxbEvent.getOptions() == null) ? 0 : jaxbEvent.getOptions().getOptionList().size();
            throw new InvalidFileException("Event '" + eventName + "' (id " + jaxbEvent.getId()
                    + ") must have exactly " + OPTIONS_PER_EVENT + " options, but "
                    + found + (found == 1 ? " was" : " were") + " found.");
        }

        List<String> optionNames = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String option : jaxbEvent.getOptions().getOptionList()) {
            String trimmed = (option == null) ? "" : option.trim();
            if (trimmed.isEmpty()) {
                throw new InvalidFileException("Event '" + eventName + "' (id " + jaxbEvent.getId()
                        + ") contains an empty option name.");
            }
            if (!seen.add(trimmed.toLowerCase())) {
                throw new InvalidFileException("Event '" + eventName + "' (id " + jaxbEvent.getId()
                        + ") contains the option '" + trimmed + "' twice. Options must be different.");
            }
            optionNames.add(trimmed);
        }
        return optionNames;
    }

    private String safeName(GmEventJaxb jaxbEvent) {
        if (jaxbEvent.getName() == null || jaxbEvent.getName().trim().isEmpty()) {
            throw new InvalidFileException("An event with id " + jaxbEvent.getId() + " has no name.");
        }
        return jaxbEvent.getName().trim();
    }

    // --- Queries ---

    @Override
    public boolean isFileLoaded() {
        return market != null;
    }

    @Override
    public List<EventDTO> getEvents() {
        requireLoaded();
        List<EventDTO> result = new ArrayList<>();
        for (Event event : market.getAllEvents()) {
            result.add(toEventDTO(event));
        }
        return result;
    }

    @Override
    public List<EventDTO> getActiveEvents() {
        requireLoaded();
        List<EventDTO> result = new ArrayList<>();
        for (Event event : market.getActiveEvents()) {
            result.add(toEventDTO(event));
        }
        return result;
    }

    @Override
    public TradeStateDTO getTradeState(int eventId) {
        requireLoaded();
        return toTradeStateDTO(market.getEventById(eventId));
    }

    // --- Command 4: buying shares ---

    @Override
    public BuyResultDTO buyShares(int eventId, int optionNumber, long shares) {
        requireLoaded();
        Event event = market.getEventById(eventId);
        int optionIndex = toValidatedOptionIndex(event, optionNumber);

        Trade trade = event.buyShares(optionIndex, shares);
        return new BuyResultDTO(trade.getOptionName(), trade.getShares(),
                trade.getSharesCost(), trade.getCommissionPaid(), toTradeStateDTO(event));
    }

    // --- Command 5: closing an event ---

    @Override
    public CloseResultDTO closeEvent(int eventId, int winningOptionNumber) {
        requireLoaded();
        Event event = market.getEventById(eventId);
        int winnerIndex = toValidatedOptionIndex(event, winningOptionNumber);

        double commissionBefore = event.getTotalCommissionCollected();
        double totalPaid = event.close(winnerIndex);
        double commissionOnClose = event.getTotalCommissionCollected() - commissionBefore;

        return new CloseResultDTO(event.getOptions().get(winnerIndex).getName(),
                totalPaid, commissionOnClose, toTradeStateDTO(event));
    }

    private int toValidatedOptionIndex(Event event, int optionNumber) {
        if (optionNumber < 1 || optionNumber > event.getOptions().size()) {
            throw new EngineOperationException("Option number " + optionNumber
                    + " does not exist in event '" + event.getName()
                    + "'. Please choose a number between 1 and " + event.getOptions().size() + ".");
        }
        return optionNumber - 1;
    }

    private void requireLoaded() {
        if (market == null) {
            throw new EngineOperationException(
                    "No system file is currently loaded. Please load a valid XML file first (command 1).");
        }
    }

    // --- Bonus: save / load full system state ---

    @Override
    public void saveState(String filePathWithoutExtension) {
        requireLoaded();
        String path = validatedStatePath(filePathWithoutExtension);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path))) {
            out.writeObject(market);
        } catch (IOException e) {
            throw new EngineOperationException("Failed to save the system state to '" + path + "': " + e.getMessage());
        }
    }

    @Override
    public void loadState(String filePathWithoutExtension) {
        String path = validatedStatePath(filePathWithoutExtension);
        File file = new File(path);
        if (!file.exists()) {
            throw new EngineOperationException("There is no saved state file at '" + path + "'.");
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            market = (Market) in.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new EngineOperationException("Failed to load a system state from '" + path
                    + "'. Make sure it was created by the save command of this system.");
        }
    }

    private String validatedStatePath(String filePathWithoutExtension) {
        if (filePathWithoutExtension == null || filePathWithoutExtension.trim().isEmpty()) {
            throw new EngineOperationException("The file path is empty. Please provide a full path (without extension).");
        }
        return filePathWithoutExtension.trim() + SAVE_FILE_EXTENSION;
    }

    // --- DTO mapping (core objects are never exposed) ---

    private EventDTO toEventDTO(Event event) {
        List<String> optionNames = new ArrayList<>();
        for (EventOption option : event.getOptions()) {
            optionNames.add(option.getName());
        }
        return new EventDTO(event.getId(), event.getName(), event.getDescription(),
                event.getCommissionPercentage(), event.getCommissionType().getDisplayName(),
                optionNames, event.isActive());
    }

    private TradeStateDTO toTradeStateDTO(Event event) {
        List<OptionStateDTO> optionStates = new ArrayList<>();
        for (int i = 0; i < event.getOptions().size(); i++) {
            EventOption option = event.getOptions().get(i);
            optionStates.add(new OptionStateDTO(option.getName(), event.getOptionPrice(i),
                    option.getTotalSharesBought()));
        }

        List<TradeDTO> tradesNewestFirst = new ArrayList<>();
        List<Trade> trades = event.getTrades();
        for (int i = trades.size() - 1; i >= 0; i--) {
            Trade trade = trades.get(i);
            tradesNewestFirst.add(new TradeDTO(trade.getOptionName(), trade.getShares(), trade.getTotalPaid()));
        }

        String winnerName = event.isActive() ? null
                : event.getOptions().get(event.getWinnerIndex()).getName();

        return new TradeStateDTO(event.getId(), event.getName(), optionStates,
                event.getAccountBalance(), event.getTotalCommissionCollected(),
                tradesNewestFirst, event.isActive(), winnerName);
    }
}
