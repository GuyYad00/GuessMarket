package engine.dto;

import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of the full trading state of an event (menu command 3):
 * option prices and share counts, the event account, total commission collected
 * and the trade history (newest first).
 */
public class TradeStateDTO {

    private final int eventId;
    private final String eventName;
    private final List<OptionStateDTO> optionStates;
    private final double accountBalance;
    private final double totalCommissionCollected;
    private final List<TradeDTO> tradesNewestFirst;
    private final boolean active;
    private final String winnerOptionName; // null while the event is active

    public TradeStateDTO(int eventId, String eventName, List<OptionStateDTO> optionStates,
                         double accountBalance, double totalCommissionCollected,
                         List<TradeDTO> tradesNewestFirst, boolean active, String winnerOptionName) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.optionStates = List.copyOf(optionStates);
        this.accountBalance = accountBalance;
        this.totalCommissionCollected = totalCommissionCollected;
        this.tradesNewestFirst = List.copyOf(tradesNewestFirst);
        this.active = active;
        this.winnerOptionName = winnerOptionName;
    }

    public int getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public List<OptionStateDTO> getOptionStates() {
        return Collections.unmodifiableList(optionStates);
    }

    public double getAccountBalance() {
        return accountBalance;
    }

    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    public List<TradeDTO> getTradesNewestFirst() {
        return Collections.unmodifiableList(tradesNewestFirst);
    }

    public boolean isActive() {
        return active;
    }

    public String getWinnerOptionName() {
        return winnerOptionName;
    }
}
