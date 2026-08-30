package com.aditya.roleplay.service;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.PlayerPersona;
import com.aditya.roleplay.storage.JsonStorageService;
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
}
