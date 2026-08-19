package engine.core;

import engine.exception.EngineOperationException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single binary event traded using the LMSR method.
 *
 * The event account tracks the net cash flow of the event's Market Maker:
 * it starts at 0 on load, every purchase adds its cost (plus commission when
 * relevant) and closing the event pays the winners out of it. A negative final
 * balance is the amount the MM actually spent subsidizing the event; a positive
 * one is his profit (per the lecturer's clarification - never reset at close).
 */
public class Event implements Serializable {

    public static final double PAYOUT_PER_SHARE = 1.0;

    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercentage;
    private final CommissionType commissionType;
    private final List<EventOption> options;
    private final int liquidity; // the LMSR 'b' parameter

    private boolean active;
    private double accountBalance;
    private double totalCommissionCollected;
    private final List<Trade> trades;
    private int winnerIndex;

    public Event(int id, String name, String description, int commissionPercentage,
                 CommissionType commissionType, List<String> optionNames, int liquidity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercentage = commissionPercentage;
        this.commissionType = commissionType;
        this.liquidity = liquidity;
        this.options = new ArrayList<>();
        for (String optionName : optionNames) {
            options.add(new EventOption(optionName));
        }
        this.active = true;
        this.accountBalance = 0;
        this.totalCommissionCollected = 0;
        this.trades = new ArrayList<>();
        this.winnerIndex = -1;
    }

    // --- LMSR math ---

    /**
     * C(q1, q2) = b * ln(e^(q1/b) + e^(q2/b))
     *
     * Written as max + b*ln(1 + e^(-|q0-q1|/b)) instead of the formula as-is.
     * Both give the same result, but e^(q/b) overflows to infinity once q/b gets
     * past ~709, which would make a large purchase print "Infinity". Subtracting
     * the larger term first keeps every exponent at zero or below.
     */
    private double costFunction(double q0, double q1) {
        double larger = Math.max(q0, q1);
        double gap = Math.abs(q0 - q1);
        return larger + liquidity * Math.log(1 + Math.exp(-gap / liquidity));
    }

    /**
     * Current price (probability, 0..1) of the option at the given index.
     *
     * p = e^(q/b) / (e^(q0/b) + e^(q1/b)), rearranged to 1 / (1 + e^((other-q)/b))
     * for the same overflow reason described on the cost function.
     */
    public double getOptionPrice(int optionIndex) {
        double q0 = options.get(0).getTotalSharesBought();
        double q1 = options.get(1).getTotalSharesBought();
        double mine = (optionIndex == 0) ? q0 : q1;
        double other = (optionIndex == 0) ? q1 : q0;
        return 1.0 / (1.0 + Math.exp((other - mine) / liquidity));
    }

    /** The initial subsidy the MM invests when the event is created: C(0,0) = b*ln(2). */
    public double getSubsidy() {
        return liquidity * Math.log(2);
    }

    // --- Trading ---

    /**
     * Buys shares of the given option. Returns the trade that was recorded.
     * Cost is the LMSR cost difference; on-purchase commission is added on top
     * of the buyer's payment and credited to the event account.
     */
    public Trade buyShares(int optionIndex, long amount) {
        requireActive("buy shares in");
        if (amount <= 0) {
            throw new EngineOperationException("The number of shares to buy must be a positive number (got " + amount + ").");
        }
        long alreadyBought = options.get(optionIndex).getTotalSharesBought();
        if (alreadyBought > Long.MAX_VALUE - amount) {
            throw new EngineOperationException("Buying " + amount
                    + " more shares would exceed the maximum number of shares this option can hold.");
        }

        double q0 = options.get(0).getTotalSharesBought();
        double q1 = options.get(1).getTotalSharesBought();
        double costBefore = costFunction(q0, q1);
        double costAfter = (optionIndex == 0)
                ? costFunction(q0 + amount, q1)
                : costFunction(q0, q1 + amount);
        double sharesCost = costAfter - costBefore;

        double commission = 0;
        if (commissionType == CommissionType.ON_PURCHASE) {
            commission = sharesCost * commissionPercentage / 100.0;
        }

        options.get(optionIndex).addShares(amount);
        accountBalance += sharesCost + commission;
        totalCommissionCollected += commission;

        Trade trade = new Trade(options.get(optionIndex).getName(), amount, sharesCost, commission);
        trades.add(trade);
        return trade;
    }

    /**
     * Closes the event with the given winning option.
     * Winners are paid PAYOUT_PER_SHARE per winning share; if the commission is
     * collected on close, it is deducted from the winners' payout and kept in
     * the event account. The account is NOT reset afterwards.
     *
     * @return the total amount paid out to the winners.
     */
    public double close(int winnerOptionIndex) {
        requireActive("close");

        long winningShares = options.get(winnerOptionIndex).getTotalSharesBought();
        double grossPayout = winningShares * PAYOUT_PER_SHARE;

        double commission = 0;
        if (commissionType == CommissionType.ON_CLOSE) {
            commission = grossPayout * commissionPercentage / 100.0;
        }

        double netPayout = grossPayout - commission;
        accountBalance -= netPayout;
        totalCommissionCollected += commission;

        winnerIndex = winnerOptionIndex;
        active = false;
        return netPayout;
    }

    private void requireActive(String action) {
        if (!active) {
            throw new EngineOperationException(
                    "Cannot " + action + " event #" + id + " ('" + name + "') - it is already closed.");
        }
    }

    // --- Getters ---

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCommissionPercentage() {
        return commissionPercentage;
    }

    public CommissionType getCommissionType() {
        return commissionType;
    }

    public List<EventOption> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public int getLiquidity() {
        return liquidity;
    }

    public boolean isActive() {
        return active;
    }

    public double getAccountBalance() {
        return accountBalance;
    }

    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    public List<Trade> getTrades() {
        return Collections.unmodifiableList(trades);
    }

    /** Index of the winning option, or -1 while the event is still active. */
    public int getWinnerIndex() {
        return winnerIndex;
    }
}
