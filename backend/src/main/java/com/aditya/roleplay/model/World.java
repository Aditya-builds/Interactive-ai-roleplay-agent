package com.aditya.roleplay.model;

import java.util.List;

public record World(
        String id,
        String name,
        String description,
        List<String> rules) {
}
