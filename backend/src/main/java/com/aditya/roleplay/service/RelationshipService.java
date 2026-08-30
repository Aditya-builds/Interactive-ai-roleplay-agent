package com.aditya.roleplay.service;

import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RelationshipService {

    public Relationship createInitialRelationship(String characterId) {
        return new Relationship(characterId, 42, 67, 12, 54, 8);
    }

    public Relationship applyStateChange(Relationship relationship, StateChange change) {
        int parsed = Integer.parseInt(change.value().trim());
        int updated = switch (change.field()) {
            case "trust" -> applyOperation(relationship.trust(), change.operation(), parsed);
            case "respect" -> applyOperation(relationship.respect(), change.operation(), parsed);
            case "affection" -> applyOperation(relationship.affection(), change.operation(), parsed);
            case "familiarity" -> applyOperation(relationship.familiarity(), change.operation(), parsed);
            case "suspicion" -> applyOperation(relationship.suspicion(), change.operation(), parsed);
            default -> throw new IllegalArgumentException("Unsupported relationship field: " + change.field());
        };

        return switch (change.field()) {
            case "trust" -> new Relationship(
                    relationship.characterId(), updated, relationship.respect(),
                    relationship.affection(), relationship.familiarity(), relationship.suspicion()).clamped();
            case "respect" -> new Relationship(
                    relationship.characterId(), relationship.trust(), updated,
                    relationship.affection(), relationship.familiarity(), relationship.suspicion()).clamped();
            case "affection" -> new Relationship(
                    relationship.characterId(), relationship.trust(), relationship.respect(),
                    updated, relationship.familiarity(), relationship.suspicion()).clamped();
            case "familiarity" -> new Relationship(
                    relationship.characterId(), relationship.trust(), relationship.respect(),
                    relationship.affection(), updated, relationship.suspicion()).clamped();
            case "suspicion" -> new Relationship(
                    relationship.characterId(), relationship.trust(), relationship.respect(),
                    relationship.affection(), relationship.familiarity(), updated).clamped();
            default -> relationship;
        };
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
