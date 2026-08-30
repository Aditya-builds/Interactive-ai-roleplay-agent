package com.aditya.roleplay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Appearance(
        Integer height,
        String hair,
        String eyes,
        String build,
        String description) {
}
