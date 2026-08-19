package engine.dto;

/** Immutable snapshot of a single trade (purchase) line. */
public class TradeDTO {

    private final String optionName;
    private final long shares;
    private final double totalPaid;

    public TradeDTO(String optionName, long shares, double totalPaid) {
        this.optionName = optionName;
        this.shares = shares;
        this.totalPaid = totalPaid;
    }

    public String getOptionName() {
        return optionName;
    }

    public long getShares() {
        return shares;
    }

    public double getTotalPaid() {
        return totalPaid;
    }
}
