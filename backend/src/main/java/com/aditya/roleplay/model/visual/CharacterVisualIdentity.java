package com.aditya.roleplay.model.visual;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CharacterVisualIdentity(
        String canonicalReferenceImage,
        String visualDescription,
        String faceDescription,
        String hairDescription,
        String eyeDescription,
        String skinDescription,
        String bodyDescription,
        String clothingDescription,
        List<String> accessories,
        String artStyle,
        String negativePrompt,
        List<String> supplementaryReferenceImages) {

    public CharacterVisualIdentity {
        accessories = accessories != null ? List.copyOf(accessories) : List.of();
        supplementaryReferenceImages = supplementaryReferenceImages != null
                ? List.copyOf(supplementaryReferenceImages)
                : List.of();
    }
}
