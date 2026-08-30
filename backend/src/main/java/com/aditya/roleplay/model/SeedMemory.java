package com.aditya.roleplay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeedMemory(
        String content,
        Double importance,
        List<String> tags,
        List<String> relatedCharacterIds) {

    public SeedMemory {
        tags = tags != null ? List.copyOf(tags) : List.of();
        relatedCharacterIds = relatedCharacterIds != null ? List.copyOf(relatedCharacterIds) : List.of();
    }
}
