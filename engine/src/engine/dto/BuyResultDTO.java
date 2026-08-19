package engine.dto;

/** Immutable result of a share purchase (menu command 4). */
public class BuyResultDTO {

    private final String optionName;
    private final long shares;
    private final double sharesCost;
    private final double commissionPaid;
    private final TradeStateDTO stateAfterPurchase;

    public BuyResultDTO(String optionName, long shares, double sharesCost,
                        double commissionPaid, TradeStateDTO stateAfterPurchase) {
        this.optionName = optionName;
        this.shares = shares;
        this.sharesCost = sharesCost;
        this.commissionPaid = commissionPaid;
        this.stateAfterPurchase = stateAfterPurchase;
    }

    public String getOptionName() {
        return optionName;
    }

    public long getShares() {
        return shares;
    }

    public double getSharesCost() {
        return sharesCost;
    }

    public double getCommissionPaid() {
        return commissionPaid;
    }

    public double getTotalPaid() {
        return sharesCost + commissionPaid;
    }

    public TradeStateDTO getStateAfterPurchase() {
        return stateAfterPurchase;
    }
}
