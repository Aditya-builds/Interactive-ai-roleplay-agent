package com.aditya.roleplay.model.visual.director;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RelationshipContext(
        String sourceId,
        String targetId,
        int trust,
        int respect,
        int affection,
        int familiarity,
        int suspicion) {
}
