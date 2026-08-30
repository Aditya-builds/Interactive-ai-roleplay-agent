package com.aditya.roleplay.model;

import java.util.List;

public record Scene(
        String location,
        String userLocation,
        String time,
        List<String> charactersPresent,
        String currentSituation,
        String currentConflict) {

    public Scene {
        if (userLocation == null || userLocation.isBlank()) {
            userLocation = location;
        }
        charactersPresent = charactersPresent != null ? List.copyOf(charactersPresent) : List.of();
    }

    public boolean userCoLocated() {
        return userLocation.equals(location);
    }
}
