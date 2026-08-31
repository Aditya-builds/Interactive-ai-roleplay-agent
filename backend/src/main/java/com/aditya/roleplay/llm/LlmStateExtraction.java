package com.aditya.roleplay.llm;

import com.aditya.roleplay.model.turn.ProposedMemory;
import com.aditya.roleplay.model.turn.ProposedStoryEvent;
import com.aditya.roleplay.model.turn.StateChange;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmStateExtraction(
        List<StateChange> stateChanges,
        List<ProposedStoryEvent> events,
        List<ProposedMemory> memories) {

    public LlmStateExtraction {
        stateChanges = stateChanges != null ? List.copyOf(stateChanges) : List.of();
        events = events != null ? List.copyOf(events) : List.of();
        memories = memories != null ? List.copyOf(memories) : List.of();
    }
}
