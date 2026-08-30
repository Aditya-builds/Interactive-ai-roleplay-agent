package com.aditya.roleplay.model.turn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProposedStoryEvent(
        String description,
        Double importance,
        List<String> participants) {

    public ProposedStoryEvent {
        participants = participants != null ? List.copyOf(participants) : List.of();
    }
}
