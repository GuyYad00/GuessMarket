package engine.api;

import engine.dto.BuyResultDTO;
import engine.dto.CloseResultDTO;
import engine.dto.EventDTO;
import engine.dto.TradeStateDTO;

import java.util.List;

/**
 * The system engine facade - the only way the UI (or any future layer)
 * interacts with the Guess Market system.
 *
 * All answers are immutable DTOs; core objects are never exposed.
 * Invalid input files throw {@link engine.exception.InvalidFileException};
 * illegal operations throw {@link engine.exception.EngineOperationException}.
 * Both are unchecked.
 */
public interface Engine {

    /**
     * Loads a system-details XML file (menu command 1).
     * A valid file completely replaces the previously loaded one;
     * an invalid file leaves the previously loaded data untouched.
     */
    void loadFile(String filePath);

    /** @return true when a valid file is currently loaded. */
    boolean isFileLoaded();

    /** @return all events in the system, in file order (menu command 2). */
    List<EventDTO> getEvents();

    /** @return only the events that are still active (open for trading). */
    List<EventDTO> getActiveEvents();

    /** @return the full trading state of the event with the given id (menu command 3). */
    TradeStateDTO getTradeState(int eventId);

    /**
     * Buys shares of an option in an active event (menu command 4).
     *
     * @param optionNumber 1-based option number as presented to the user.
     */
    BuyResultDTO buyShares(int eventId, int optionNumber, long shares);

    /**
     * Closes (resolves) an active event (menu command 5).
     *
     * @param winningOptionNumber 1-based option number as presented to the user.
     */
    CloseResultDTO closeEvent(int eventId, int winningOptionNumber);

    /** Bonus: saves the complete system state to the given path (extension is added automatically). */
    void saveState(String filePathWithoutExtension);

    /** Bonus: loads a complete system state previously saved by {@link #saveState}. */
    void loadState(String filePathWithoutExtension);
}
