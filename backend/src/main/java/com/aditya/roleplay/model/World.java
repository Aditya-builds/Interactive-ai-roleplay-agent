package com.aditya.roleplay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record World(
        String id,
        String name,
        String description,
        List<String> rules,
        List<String> races,
        List<String> factions,
        List<WorldLocation> locations,
        Map<String, Object> systems) {

    public World {
        rules = rules != null ? List.copyOf(rules) : List.of();
        races = races != null ? List.copyOf(races) : List.of();
        factions = factions != null ? List.copyOf(factions) : List.of();
        locations = locations != null ? List.copyOf(locations) : List.of();
        systems = systems != null ? Map.copyOf(systems) : Map.of();
    }

    /** Backward-compatible constructor for legacy world JSON. */
    public World(String id, String name, String description, List<String> rules) {
        this(id, name, description, rules, List.of(), List.of(), List.of(), Map.of());
    }

    public boolean isValidLocation(String locationId) {
        if (locationId == null || locationId.isBlank()) {
            return false;
        }
        if (locations.isEmpty()) {
            return true;
        }
        return locations.stream().anyMatch(loc -> locationId.equals(loc.id()));
    }
}
