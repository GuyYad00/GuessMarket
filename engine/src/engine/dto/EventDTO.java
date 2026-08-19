package engine.dto;

import java.util.Collections;
import java.util.List;

/** Immutable snapshot of a single event's general details (menu command 2). */
public class EventDTO {

    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercentage;
    private final String commissionType;
    private final List<String> optionNames;
    private final boolean active;

    public EventDTO(int id, String name, String description, int commissionPercentage,
                    String commissionType, List<String> optionNames, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercentage = commissionPercentage;
        this.commissionType = commissionType;
        this.optionNames = List.copyOf(optionNames);
        this.active = active;
    }

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

    public String getCommissionType() {
        return commissionType;
    }

    public List<String> getOptionNames() {
        return Collections.unmodifiableList(optionNames);
    }

    public boolean isActive() {
        return active;
    }
}
