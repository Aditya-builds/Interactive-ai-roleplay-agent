package com.aditya.roleplay.model.turn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProposedMemory(
        String content,
        Double importance,
        List<String> tags,
        List<String> relatedCharacterIds) {

    public ProposedMemory {
        tags = tags != null ? List.copyOf(tags) : List.of();
        relatedCharacterIds = relatedCharacterIds != null ? List.copyOf(relatedCharacterIds) : List.of();
    }
}
