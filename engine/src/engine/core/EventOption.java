package engine.core;

import java.io.Serializable;

public class EventOption implements Serializable {

    private final String name;
    private long totalSharesBought;

    public EventOption(String name) {
        this.name = name;
        this.totalSharesBought = 0;
    }

    public String getName() {
        return name;
    }

    public long getTotalSharesBought() {
        return totalSharesBought;
    }

    public void addShares(long amount) {
        totalSharesBought += amount;
    }
}
