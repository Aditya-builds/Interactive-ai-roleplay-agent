package com.aditya.roleplay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorldLocation(
        String id,
        String name,
        String description) {
}
