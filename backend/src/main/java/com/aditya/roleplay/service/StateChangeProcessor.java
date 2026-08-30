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
import com.aditya.roleplay.model.turn.StateChangeOperation;
import com.aditya.roleplay.model.turn.StateChangeType;
import com.aditya.roleplay.llm.LlmTurnResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

    @ConfigProperty(name = "roleplay.state.max-changes-per-turn", defaultValue = "5")
    int maxChangesPerTurn;

    public ConversationState apply(
            ConversationState state,
            RoleplayCharacter characterDefinition,
            LlmTurnResult turnResult,
            Set<String> allowedRelationshipTargets) {

        ConversationState current = state.ensureCharacterState(characterDefinition);

        List<Relationship> relationships = new ArrayList<>(current.relationships());
        Scene scene = current.scene();
        CharacterRuntimeState characterState = current.characterState();
        List<StoryEvent> events = new ArrayList<>(current.events());
        List<StoryMemoryEntry> memories = new ArrayList<>(current.memories());

        int applied = 0;
        for (StateChange change : turnResult.stateChanges()) {
            if (applied >= maxChangesPerTurn) {
                break;
            }
            StateChange normalized = normalizeChange(change, current.characterId());
            StateChangeValidator.ValidationResult validation = validator.validate(
                    normalized,
                    current.characterId(),
                    allowedRelationshipTargets);
            if (!validation.valid()) {
                LOG.fine(() -> "Rejected state change: " + validation.reason());
                continue;
            }

            switch (normalized.type()) {
                case RELATIONSHIP -> {
                    relationships = relationshipService.applyStateChange(relationships, normalized);
                    applied++;
                }
                case SCENE -> {
                    scene = storyStateService.applySceneChange(scene, normalized);
                    applied++;
                }
                case HEALTH -> {
                    characterState = storyStateService.applyHealthChange(characterState, normalized);
                    applied++;
                }
                case LOCATION -> {
                    scene = storyStateService.applySceneChange(scene, toSceneLocationChange(normalized));
                    applied++;
                }
                case STATUS -> {
                    characterState = storyStateService.applyStatusChange(characterState, normalized);
                    applied++;
                }
                case EMOTION -> {
                    characterState = storyStateService.applyEmotionChange(characterState, normalized);
                    applied++;
                }
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
                    importance,
                    proposed.tags(),
                    proposed.relatedCharacterIds()));
        }
        if (memories.size() > maxMemories) {
            memories = trimMemories(memories, maxMemories);
        }

        return new ConversationState(
                current.characterId(),
                characterState,
                scene,
                relationships,
                events,
                memories);
    }

    private StateChange normalizeChange(StateChange change, String conversationCharacterId) {
        if (change.type() == StateChangeType.LOCATION) {
            return toSceneLocationChange(change);
        }
        if (change.type() == StateChangeType.RELATIONSHIP && conversationCharacterId.equals(change.targetId())) {
            return new StateChange(
                    change.type(),
                    "user",
                    change.field(),
                    change.operation(),
                    change.value());
        }
        return change;
    }

    private StateChange toSceneLocationChange(StateChange change) {
        return new StateChange(
                StateChangeType.SCENE,
                "scene",
                "location",
                StateChangeOperation.SET,
                change.value());
    }

    private List<StoryMemoryEntry> trimMemories(List<StoryMemoryEntry> memories, int max) {
        List<StoryMemoryEntry> sorted = new ArrayList<>(memories);
        sorted.sort((a, b) -> Double.compare(
                b.importance() != null ? b.importance() : 0.0,
                a.importance() != null ? a.importance() : 0.0));
        List<StoryMemoryEntry> kept = new ArrayList<>(sorted.subList(0, Math.min(max, sorted.size())));
        kept.sort((a, b) -> a.createdAt().compareTo(b.createdAt()));
        return kept;
    }

    public record ConversationState(
            String characterId,
            CharacterRuntimeState characterState,
            Scene scene,
            List<Relationship> relationships,
            List<StoryEvent> events,
            List<StoryMemoryEntry> memories) {

        public ConversationState {
            relationships = relationships != null ? List.copyOf(relationships) : List.of();
            events = events != null ? List.copyOf(events) : List.of();
            memories = memories != null ? List.copyOf(memories) : List.of();
        }

        public ConversationState ensureCharacterState(RoleplayCharacter character) {
            if (characterState != null) {
                return this;
            }
            CharacterHealth health = character.health() != null
                    ? character.health()
                    : new CharacterHealth(100, 100);
            return new ConversationState(
                    characterId,
                    new CharacterRuntimeState(character.id(), health, null, null),
                    scene,
                    relationships,
                    events,
                    memories);
        }
    }
}
