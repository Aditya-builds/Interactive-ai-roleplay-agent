package com.aditya.roleplay.service;

import com.aditya.roleplay.model.CharacterHealth;
import com.aditya.roleplay.model.CharacterRuntimeState;
import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Scene;
import com.aditya.roleplay.model.StoryEvent;
import com.aditya.roleplay.model.StoryMemoryEntry;
import com.aditya.roleplay.model.turn.ProposedMemory;
import com.aditya.roleplay.model.turn.ProposedStoryEvent;
import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeType;
import com.aditya.roleplay.llm.LlmTurnResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
public class StateChangeProcessor {

    private static final Logger LOG = Logger.getLogger(StateChangeProcessor.class.getName());

    @Inject
    StateChangeValidator validator;

    @Inject
    RelationshipService relationshipService;

    @Inject
    StoryStateService storyStateService;

    @ConfigProperty(name = "roleplay.memory.importance-threshold", defaultValue = "0.5")
    double memoryImportanceThreshold;

    @ConfigProperty(name = "roleplay.memory.max-count", defaultValue = "30")
    int maxMemories;

    @ConfigProperty(name = "roleplay.events.max-count", defaultValue = "50")
    int maxEvents;

    public ConversationState apply(
            ConversationState state,
            RoleplayCharacter characterDefinition,
            LlmTurnResult turnResult) {

        ConversationState current = state.ensureCharacterState(characterDefinition);

        Relationship relationship = current.relationship();
        Scene scene = current.scene();
        CharacterRuntimeState characterState = current.characterState();
        List<StoryEvent> events = new ArrayList<>(current.events());
        List<StoryMemoryEntry> memories = new ArrayList<>(current.memories());

        for (StateChange change : turnResult.stateChanges()) {
            StateChangeValidator.ValidationResult validation = validator.validate(change, current.characterId());
            if (!validation.valid()) {
                LOG.fine(() -> "Rejected state change: " + validation.reason());
                continue;
            }

            switch (change.type()) {
                case RELATIONSHIP -> relationship = relationshipService.applyStateChange(relationship, change);
                case SCENE -> scene = storyStateService.applySceneChange(scene, change);
                case HEALTH -> characterState = storyStateService.applyHealthChange(characterState, change);
                case LOCATION -> {
                    characterState = storyStateService.applyLocationChange(characterState, change);
                    scene = storyStateService.applySceneChange(scene, new StateChange(
                            StateChangeType.SCENE,
                            "scene",
                            "location",
                            change.operation(),
                            change.value()));
                }
                case STATUS -> characterState = storyStateService.applyStatusChange(characterState, change);
                case EMOTION -> characterState = storyStateService.applyEmotionChange(characterState, change);
            }
        }

        Instant now = Instant.now();
        for (ProposedStoryEvent proposed : turnResult.events()) {
            if (proposed.description() == null || proposed.description().isBlank()) {
                continue;
            }
            events.add(new StoryEvent(
                    UUID.randomUUID().toString(),
                    proposed.description().trim(),
                    now,
                    proposed.participants(),
                    proposed.importance()));
        }
        if (events.size() > maxEvents) {
            events = new ArrayList<>(events.subList(events.size() - maxEvents, events.size()));
        }

        for (ProposedMemory proposed : turnResult.memories()) {
            if (proposed.content() == null || proposed.content().isBlank()) {
                continue;
            }
            double importance = proposed.importance() != null ? proposed.importance() : 0.0;
            if (importance < memoryImportanceThreshold) {
                continue;
            }
            memories.add(new StoryMemoryEntry(
                    UUID.randomUUID().toString(),
                    proposed.content().trim(),
                    now,
                    "llm",
                    importance));
        }
        if (memories.size() > maxMemories) {
            memories = new ArrayList<>(memories.subList(memories.size() - maxMemories, memories.size()));
        }

        return new ConversationState(
                current.characterId(),
                characterState,
                scene,
                relationship,
                events,
                memories);
    }

    public record ConversationState(
            String characterId,
            CharacterRuntimeState characterState,
            Scene scene,
            Relationship relationship,
            List<StoryEvent> events,
            List<StoryMemoryEntry> memories) {

        public ConversationState {
            events = events != null ? List.copyOf(events) : List.of();
            memories = memories != null ? List.copyOf(memories) : List.of();
        }

        public ConversationState ensureCharacterState(RoleplayCharacter character) {
            if (characterState != null) {
                return this;
            }
            String location = character.presence() != null
                    ? character.presence().defaultLocation()
                    : "unknown";
            CharacterHealth health = character.health() != null
                    ? character.health()
                    : new CharacterHealth(100, 100);
            return new ConversationState(
                    characterId,
                    new CharacterRuntimeState(character.id(), health, location, null, null),
                    scene,
                    relationship,
                    events,
                    memories);
        }
    }
}
