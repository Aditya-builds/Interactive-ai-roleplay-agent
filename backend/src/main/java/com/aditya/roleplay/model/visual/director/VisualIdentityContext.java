package com.aditya.roleplay.model.visual.director;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VisualIdentityContext(
        String characterId,
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
        String negativePrompt) {

    public VisualIdentityContext {
        accessories = accessories != null ? List.copyOf(accessories) : List.of();
    }
}
