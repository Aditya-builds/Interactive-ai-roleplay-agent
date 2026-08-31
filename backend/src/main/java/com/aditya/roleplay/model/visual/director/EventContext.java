package com.aditya.roleplay.model.visual.director;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventContext(String description, Double importance) {
}
