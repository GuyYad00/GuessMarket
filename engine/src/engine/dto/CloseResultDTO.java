package engine.dto;

/** Immutable result of closing (resolving) an event (menu command 5). */
public class CloseResultDTO {

    private final String winnerOptionName;
    private final double totalPaidToWinners;
    private final double commissionCollectedOnClose;
    private final TradeStateDTO stateAfterClose;

    public CloseResultDTO(String winnerOptionName, double totalPaidToWinners,
                          double commissionCollectedOnClose, TradeStateDTO stateAfterClose) {
        this.winnerOptionName = winnerOptionName;
        this.totalPaidToWinners = totalPaidToWinners;
        this.commissionCollectedOnClose = commissionCollectedOnClose;
        this.stateAfterClose = stateAfterClose;
    }

    public String getWinnerOptionName() {
        return winnerOptionName;
    }

    public double getTotalPaidToWinners() {
        return totalPaidToWinners;
    }

    public double getCommissionCollectedOnClose() {
        return commissionCollectedOnClose;
    }

    public TradeStateDTO getStateAfterClose() {
        return stateAfterClose;
    }
}
