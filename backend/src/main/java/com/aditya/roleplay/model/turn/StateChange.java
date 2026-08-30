package com.aditya.roleplay.model.turn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StateChange(
        StateChangeType type,
        String targetId,
        String field,
        StateChangeOperation operation,
        String value) {
}
