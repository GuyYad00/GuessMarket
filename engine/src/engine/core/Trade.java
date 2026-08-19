package engine.core;

import java.io.Serializable;

/**
 * A single purchase made in an event (exercise 1 supports buying only).
 */
public class Trade implements Serializable {

    private final String optionName;
    private final long shares;
    private final double sharesCost;
    private final double commissionPaid;

    public Trade(String optionName, long shares, double sharesCost, double commissionPaid) {
        this.optionName = optionName;
        this.shares = shares;
        this.sharesCost = sharesCost;
        this.commissionPaid = commissionPaid;
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
}
