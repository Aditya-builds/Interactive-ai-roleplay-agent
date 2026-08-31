package com.aditya.roleplay.model.visual.director;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VisualDirectorRequest(
        String conversationId,
        String focalCharacterId,
        String playerPersonaName,
        boolean explicitGeneration,
        SceneContext scene,
        CharacterStateContext characterState,
        List<MessageContext> recentMessages,
        List<EventContext> recentEvents,
        List<RelationshipContext> relationships,
        List<CandidateCharacter> candidateCharacters,
        List<VisualIdentityContext> visualIdentities) {

    public VisualDirectorRequest {
        recentMessages = recentMessages != null ? List.copyOf(recentMessages) : List.of();
        recentEvents = recentEvents != null ? List.copyOf(recentEvents) : List.of();
        relationships = relationships != null ? List.copyOf(relationships) : List.of();
        candidateCharacters = candidateCharacters != null ? List.copyOf(candidateCharacters) : List.of();
        visualIdentities = visualIdentities != null ? List.copyOf(visualIdentities) : List.of();
    }
}
