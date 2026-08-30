package com.aditya.roleplay.model;

import java.util.List;

public record Scene(
        String location,
        String time,
        List<String> charactersPresent,
        String currentSituation,
        String currentConflict) {
}
