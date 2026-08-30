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
        return apply(state, characterDefinition, turnResult, allowedRelationshipTargets, "user", null);
    }

    public ConversationState apply(
            ConversationState state,
            RoleplayCharacter characterDefinition,
            LlmTurnResult turnResult,
            Set<String> allowedRelationshipTargets,
            String playerPersonaId,
            com.aditya.roleplay.model.World world) {

        ConversationState current = state.ensureCharacterState(characterDefinition);
        current = reconcileNpcLocation(current);

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
            if (isBlockedPlayerPersonaChange(change, playerPersonaId)) {
                LOG.fine(() -> "Rejected player persona state change: " + change.type() + " on " + change.targetId());
                continue;
            }
            StateChange normalized = normalizeChange(change, current.characterId(), playerPersonaId);
            StateChangeValidator.ValidationResult validation = validator.validate(
                    normalized,
                    current.characterId(),
                    allowedRelationshipTargets,
                    scene,
                    world);
            if (!validation.valid()) {
                LOG.fine(() -> "Rejected state change: " + validation.reason());
                continue;
            }

            switch (normalized.type()) {
                case RELATIONSHIP -> {
                    relationships = relationshipService.applyStateChange(
                            relationships, normalized, current.characterId());
                    applied++;
                }
                case SCENE -> {
                    scene = storyStateService.applySceneChange(scene, normalized);
                    if ("location".equals(normalized.field())) {
                        characterState = storyStateService.applyLocationChange(characterState, locationChangeFor(
                                current.characterId(), normalized.value()));
                    }
                    applied++;
                }
                case HEALTH -> {
                    characterState = storyStateService.applyHealthChange(characterState, normalized);
                    applied++;
                }
                case LOCATION -> {
                    if ("user".equals(normalized.targetId())) {
                        scene = storyStateService.applyUserLocationChange(scene, normalized.value());
                    } else {
                        scene = storyStateService.applyNpcLocationChange(scene, normalized.value());
                        characterState = storyStateService.applyLocationChange(characterState, locationChangeFor(
                                current.characterId(), normalized.value()));
                    }
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

        characterState = storyStateService.reconcileCharacterLocation(characterState, scene);

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
            if (hasDuplicateMemory(memories, proposed.content())) {
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

    private ConversationState reconcileNpcLocation(ConversationState state) {
        CharacterRuntimeState reconciled = storyStateService.reconcileCharacterLocation(
                state.characterState(), state.scene());
        return new ConversationState(
                state.characterId(),
                reconciled,
                state.scene(),
                state.relationships(),
                state.events(),
                state.memories());
    }

    private StateChange locationChangeFor(String characterId, String location) {
        return new StateChange(
                StateChangeType.LOCATION,
                characterId,
                "location",
                StateChangeOperation.SET,
                location);
    }

    private StateChange normalizeChange(StateChange change, String conversationCharacterId, String playerPersonaId) {
        if (change.type() == StateChangeType.LOCATION) {
            if (playerPersonaId != null && playerPersonaId.equals(change.targetId())) {
                return new StateChange(
                        change.type(),
                        "user",
                        change.field(),
                        change.operation(),
                        change.value());
            }
            return change;
        }
        if (change.type() == StateChangeType.RELATIONSHIP) {
            String targetId = change.targetId();
            if ("user".equals(targetId) && playerPersonaId != null && !playerPersonaId.isBlank()) {
                targetId = playerPersonaId;
            }
            if (conversationCharacterId.equals(change.targetId())) {
                targetId = playerPersonaId != null && !playerPersonaId.isBlank() ? playerPersonaId : "user";
            }
            return new StateChange(change.type(), targetId, change.field(), change.operation(), change.value());
        }
        return change;
    }

    private boolean isBlockedPlayerPersonaChange(StateChange change, String playerPersonaId) {
        if (change.type() == StateChangeType.RELATIONSHIP || change.type() == StateChangeType.SCENE) {
            return false;
        }
        if ("user".equals(change.targetId())) {
            return change.type() != StateChangeType.LOCATION;
        }
        return playerPersonaId != null && playerPersonaId.equals(change.targetId());
    }

    private boolean hasDuplicateMemory(List<StoryMemoryEntry> memories, String content) {
        String normalized = content.trim().toLowerCase();
        return memories.stream().anyMatch(m -> m.content().trim().toLowerCase().equals(normalized));
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
            String location = scene != null ? scene.location()
                    : (character.presence() != null ? character.presence().defaultLocation() : "unknown");
            return new ConversationState(
                    characterId,
                    new CharacterRuntimeState(character.id(), health, location, null, null),
                    scene,
                    relationships,
                    events,
                    memories);
        }
    }
}
