package com.aditya.roleplay.service;

import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.RoleplayCharacter;
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
                    .map(Relationship::clamped)
                    .toList();
        }
        return List.of(new Relationship("user", 40, 50, 10, 20, 5));
    }

    public List<Relationship> applyStateChange(List<Relationship> relationships, StateChange change) {
        List<Relationship> updated = new ArrayList<>(relationships);
        String targetId = change.targetId();
        int index = indexOfTarget(updated, targetId);
        Relationship current = index >= 0
                ? updated.get(index)
                : new Relationship(targetId, 40, 50, 10, 20, 5);

        Relationship modified = applyFieldChange(current, change).clamped();
        if (index >= 0) {
            updated.set(index, modified);
        } else {
            updated.add(modified);
        }
        return List.copyOf(updated);
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
                    relationship.targetId(), value, relationship.respect(),
                    relationship.affection(), relationship.familiarity(), relationship.suspicion());
            case "respect" -> new Relationship(
                    relationship.targetId(), relationship.trust(), value,
                    relationship.affection(), relationship.familiarity(), relationship.suspicion());
            case "affection" -> new Relationship(
                    relationship.targetId(), relationship.trust(), relationship.respect(),
                    value, relationship.familiarity(), relationship.suspicion());
            case "familiarity" -> new Relationship(
                    relationship.targetId(), relationship.trust(), relationship.respect(),
                    relationship.affection(), value, relationship.suspicion());
            case "suspicion" -> new Relationship(
                    relationship.targetId(), relationship.trust(), relationship.respect(),
                    relationship.affection(), relationship.familiarity(), value);
            default -> relationship;
        };
    }

    private int indexOfTarget(List<Relationship> relationships, String targetId) {
        for (int i = 0; i < relationships.size(); i++) {
            if (targetId.equals(relationships.get(i).targetId())) {
                return i;
            }
        }
        return -1;
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
