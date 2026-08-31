package com.aditya.roleplay.model.visual.director;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CandidateCharacter(String characterId, String name, boolean inScene) {
}
