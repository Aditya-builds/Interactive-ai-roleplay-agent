package com.aditya.roleplay.service;

import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Story;
import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class RelationshipService {

    public List<Relationship> createInitialRelationships(RoleplayCharacter character) {
        if (character.defaultRelationships() != null && !character.defaultRelationships().isEmpty()) {
            return character.defaultRelationships().stream()
                    .map(rel -> rel.withSourceId(character.id()).clamped())
                    .toList();
        }
        return List.of(new Relationship(character.id(), "user", 40, 50, 10, 20, 5));
    }

    public List<Relationship> createInitialRelationshipsFromStory(
            Story story,
            RoleplayCharacter focalCharacter,
            String playerPersonaId) {
        if (story.startingRelationships() != null && !story.startingRelationships().isEmpty()) {
            return story.startingRelationships().stream()
                    .map(rel -> normalizeStoryRelationship(rel, focalCharacter.id(), playerPersonaId).clamped())
                    .toList();
        }
        return List.of(new Relationship(focalCharacter.id(), playerPersonaId, 10, 20, 0, 0, 35));
    }

    public List<Relationship> applyStateChange(
            List<Relationship> relationships,
            StateChange change,
            String focalCharacterId) {
        List<Relationship> updated = new ArrayList<>(relationships);
        String targetId = change.targetId();
        int index = indexOfRelationship(updated, focalCharacterId, targetId);
        Relationship current = index >= 0
                ? updated.get(index)
                : new Relationship(focalCharacterId, targetId, 40, 50, 10, 20, 5);

        Relationship modified = applyFieldChange(current, change).clamped();
        if (index >= 0) {
            updated.set(index, modified);
        } else {
            updated.add(modified);
        }
        return List.copyOf(updated);
    }

    private Relationship normalizeStoryRelationship(
            Relationship relationship,
            String focalCharacterId,
            String playerPersonaId) {
        String sourceId = relationship.sourceId() != null ? relationship.sourceId() : focalCharacterId;
        String targetId = relationship.targetId();
        if ("user".equals(targetId) || "player".equals(targetId)) {
            targetId = playerPersonaId;
        }
        return new Relationship(
                sourceId,
                targetId,
                relationship.trust(),
                relationship.respect(),
                relationship.affection(),
                relationship.familiarity(),
                relationship.suspicion());
    }

    private Relationship applyFieldChange(Relationship relationship, StateChange change) {
        int parsed = Integer.parseInt(change.value().trim());
        int value = switch (change.field()) {
            case "trust" -> applyOperation(relationship.trust(), change.operation(), parsed);
            case "respect" -> applyOperation(relationship.respect(), change.operation(), parsed);
            case "affection" -> applyOperation(relationship.affection(), change.operation(), parsed);
            case "familiarity" -> applyOperation(relationship.familiarity(), change.operation(), parsed);
            case "suspicion" -> applyOperation(relationship.suspicion(), change.operation(), parsed);
            default -> throw new IllegalArgumentException("Unsupported relationship field: " + change.field());
        };

        return switch (change.field()) {
            case "trust" -> new Relationship(
                    relationship.sourceId(), relationship.targetId(), value, relationship.respect(),
                    relationship.affection(), relationship.familiarity(), relationship.suspicion());
            case "respect" -> new Relationship(
                    relationship.sourceId(), relationship.targetId(), relationship.trust(), value,
                    relationship.affection(), relationship.familiarity(), relationship.suspicion());
            case "affection" -> new Relationship(
                    relationship.sourceId(), relationship.targetId(), relationship.trust(), relationship.respect(),
                    value, relationship.familiarity(), relationship.suspicion());
            case "familiarity" -> new Relationship(
                    relationship.sourceId(), relationship.targetId(), relationship.trust(), relationship.respect(),
                    relationship.affection(), value, relationship.suspicion());
            case "suspicion" -> new Relationship(
                    relationship.sourceId(), relationship.targetId(), relationship.trust(), relationship.respect(),
                    relationship.affection(), relationship.familiarity(), value);
            default -> relationship;
        };
    }

    private int indexOfRelationship(List<Relationship> relationships, String sourceId, String targetId) {
        for (int i = 0; i < relationships.size(); i++) {
            Relationship relationship = relationships.get(i);
            if (targetId.equals(relationship.targetId()) && matchesSource(relationship, sourceId)) {
                return i;
            }
        }
        return -1;
    }

    private boolean matchesSource(Relationship relationship, String sourceId) {
        return relationship.sourceId() == null || sourceId.equals(relationship.sourceId());
    }

    private int applyOperation(int current, StateChangeOperation operation, int value) {
        return switch (operation) {
            case INCREASE -> current + value;
            case DECREASE -> current - value;
            case SET -> value;
            default -> current;
        };
    }
}
