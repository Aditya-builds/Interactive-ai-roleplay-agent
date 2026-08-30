package com.aditya.roleplay.model.turn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProposedMemory(
        String content,
        Double importance) {
}
