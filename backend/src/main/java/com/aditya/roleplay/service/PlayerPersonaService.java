package com.aditya.roleplay.service;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.ActorProfile;
import com.aditya.roleplay.model.CreatePersonaRequest;
import com.aditya.roleplay.model.PlayerPersona;
import com.aditya.roleplay.storage.JsonStorageService;
import com.aditya.roleplay.util.SlugIdGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class PlayerPersonaService {

    @Inject
    JsonStorageService storage;

    public List<PlayerPersona> listPersonas() {
        return storage.loadPersonas();
    }

    public PlayerPersona requirePersona(String id) {
        return storage.loadPersona(id)
                .orElseThrow(() -> new RoleplayException("Player persona not found: " + id, "PERSONA_NOT_FOUND", 404));
    }

    public PlayerPersona createPersona(CreatePersonaRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new RoleplayException("Persona name is required", "INVALID_REQUEST", 400);
        }

        String personaId = request.id() != null && !request.id().isBlank()
                ? request.id().trim()
                : SlugIdGenerator.fromName(request.name());
        if (storage.loadPersona(personaId).isPresent()) {
            personaId = SlugIdGenerator.uniqueFromName(request.name(), id -> storage.loadPersona(id).isPresent());
        }

        PlayerPersona persona = new PlayerPersona(
                personaId,
                request.name().trim(),
                request.worldId() != null ? request.worldId().trim() : null,
                request.imageUrl(),
                new ActorProfile(
                        request.description() != null ? request.description().trim() : "",
                        request.personality() != null ? request.personality() : java.util.List.of(),
                        request.background() != null ? request.background().trim() : "",
                        java.util.List.of(),
                        request.speakingStyle() != null ? request.speakingStyle().trim() : ""),
                null,
                java.util.List.of(),
                java.util.List.of());

        return storage.savePersona(persona);
    }
}
