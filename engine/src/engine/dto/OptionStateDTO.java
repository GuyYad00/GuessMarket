package engine.dto;

/** Immutable snapshot of a single option's trading state. */
public class OptionStateDTO {

    private final String name;
    private final double price;
    private final long totalSharesBought;

    public OptionStateDTO(String name, double price, long totalSharesBought) {
        this.name = name;
        this.price = price;
        this.totalSharesBought = totalSharesBought;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public long getTotalSharesBought() {
        return totalSharesBought;
    }
}
