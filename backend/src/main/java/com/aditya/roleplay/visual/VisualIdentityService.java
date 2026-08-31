package com.aditya.roleplay.visual;

import com.aditya.roleplay.model.Appearance;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.visual.CharacterVisualIdentity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves immutable character visual identity from character definitions.
 * Canonical identity is never modified by scene state or roleplay turns.
 */
@ApplicationScoped
public class VisualIdentityService {

    public CharacterVisualIdentity resolve(RoleplayCharacter character) {
        if (character.visualIdentity() != null) {
            return character.visualIdentity();
        }
        return fallbackFromAppearance(character);
    }

    public List<String> resolveReferenceImagePaths(RoleplayCharacter character, VisualImageStorageService storage) {
        CharacterVisualIdentity identity = resolve(character);
        List<String> paths = new ArrayList<>();

        if (identity.canonicalReferenceImage() != null && !identity.canonicalReferenceImage().isBlank()) {
            PathFromReference path = resolveReferencePath(character.id(), identity.canonicalReferenceImage(), storage);
            if (path != null) {
                paths.add(path.filesystemPath());
            }
        } else if (storage.canonicalReferenceExists(character.id())) {
            paths.add(storage.canonicalReferencePath(character.id()).toString());
        }

        for (String supplementary : identity.supplementaryReferenceImages()) {
            PathFromReference path = resolveReferencePath(character.id(), supplementary, storage);
            if (path != null) {
                paths.add(path.filesystemPath());
            }
        }

        return List.copyOf(paths);
    }

    private static PathFromReference resolveReferencePath(
            String characterId,
            String reference,
            VisualImageStorageService storage) {
        if (reference.startsWith("/api/visuals/references/")) {
            if (storage.canonicalReferenceExists(characterId)) {
                return new PathFromReference(storage.canonicalReferencePath(characterId).toString());
            }
            return null;
        }
        return new PathFromReference(reference);
    }

    private CharacterVisualIdentity fallbackFromAppearance(RoleplayCharacter character) {
        Appearance appearance = character.appearance();
        String hair = appearance != null ? appearance.hair() : null;
        String eyes = appearance != null ? appearance.eyes() : null;
        String build = appearance != null ? appearance.build() : null;
        String description = appearance != null ? appearance.description() : null;

        String canonicalRef = "/api/visuals/references/" + character.id();

        return new CharacterVisualIdentity(
                canonicalRef,
                description,
                null,
                hair,
                eyes,
                null,
                build,
                null,
                List.of(),
                "dark fantasy, cinematic lighting, painterly realism",
                "different face, different hair, different eyes, different body, inconsistent character",
                List.of());
    }

    private record PathFromReference(String filesystemPath) {
    }
}
