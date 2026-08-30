package com.aditya.roleplay.service;

import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import com.aditya.roleplay.model.turn.StateChangeType;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Set;
import java.util.regex.Pattern;

@ApplicationScoped
public class StateChangeValidator {

    private static final Pattern LOCATION_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");

    private static final Set<String> RELATIONSHIP_FIELDS = Set.of(
            "trust", "respect", "affection", "familiarity", "suspicion");

    private static final Set<String> SCENE_FIELDS = Set.of(
            "location", "time", "currentSituation", "currentConflict");

    private static final Set<StateChangeOperation> NUMERIC_OPS = Set.of(
            StateChangeOperation.INCREASE,
            StateChangeOperation.DECREASE,
            StateChangeOperation.SET);

    private static final Set<StateChangeOperation> SET_ONLY_OPS = Set.of(StateChangeOperation.SET);

    @ConfigProperty(name = "roleplay.state.max-relationship-delta", defaultValue = "10")
    int maxRelationshipDelta;

    @ConfigProperty(name = "roleplay.state.max-health-delta", defaultValue = "50")
    int maxHealthDelta;

    public ValidationResult validate(StateChange change, String conversationCharacterId) {
        if (change == null) {
            return ValidationResult.rejected("State change is null");
        }
        if (change.type() == null) {
            return ValidationResult.rejected("State change type is required");
        }
        if (change.targetId() == null || change.targetId().isBlank()) {
            return ValidationResult.rejected("State change targetId is required");
        }
        if (change.field() == null || change.field().isBlank()) {
            return ValidationResult.rejected("State change field is required");
        }
        if (change.operation() == null) {
            return ValidationResult.rejected("State change operation is required");
        }
        if (change.value() == null || change.value().isBlank()) {
            return ValidationResult.rejected("State change value is required");
        }

        return switch (change.type()) {
            case RELATIONSHIP -> validateRelationship(change, conversationCharacterId);
            case SCENE -> validateScene(change);
            case HEALTH -> validateHealth(change, conversationCharacterId);
            case LOCATION -> validateLocation(change, conversationCharacterId);
            case STATUS -> validateStatus(change, conversationCharacterId);
            case EMOTION -> validateEmotion(change, conversationCharacterId);
        };
    }

    private ValidationResult validateRelationship(StateChange change, String conversationCharacterId) {
        if (!conversationCharacterId.equals(change.targetId())) {
            return ValidationResult.rejected("Relationship changes must target the conversation character: " + conversationCharacterId);
        }
        if (!RELATIONSHIP_FIELDS.contains(change.field())) {
            return ValidationResult.rejected("Unsupported relationship field: " + change.field());
        }
        if (!NUMERIC_OPS.contains(change.operation())) {
            return ValidationResult.rejected("Unsupported relationship operation: " + change.operation());
        }
        int magnitude = parsePositiveMagnitude(change);
        if (change.operation() != StateChangeOperation.SET && magnitude > maxRelationshipDelta) {
            return ValidationResult.rejected("Relationship delta exceeds max allowed per turn: " + maxRelationshipDelta);
        }
        if (change.operation() == StateChangeOperation.SET && (magnitude < 0 || magnitude > 100)) {
            return ValidationResult.rejected("Relationship SET value must be between 0 and 100");
        }
        return ValidationResult.accepted();
    }

    private ValidationResult validateScene(StateChange change) {
        if (!"scene".equals(change.targetId())) {
            return ValidationResult.rejected("Scene changes must use targetId 'scene'");
        }
        if (!SCENE_FIELDS.contains(change.field())) {
            return ValidationResult.rejected("Unsupported scene field: " + change.field());
        }
        if (change.operation() != StateChangeOperation.SET) {
            return ValidationResult.rejected("Scene changes only support SET");
        }
        if (change.field().equals("location") && !LOCATION_PATTERN.matcher(change.value()).matches()) {
            return ValidationResult.rejected("Invalid scene location format");
        }
        return ValidationResult.accepted();
    }

    private ValidationResult validateHealth(StateChange change, String conversationCharacterId) {
        if (!conversationCharacterId.equals(change.targetId())) {
            return ValidationResult.rejected("Health changes must target the conversation character: " + conversationCharacterId);
        }
        if (!"current".equals(change.field())) {
            return ValidationResult.rejected("Health changes only support field 'current'");
        }
        if (!NUMERIC_OPS.contains(change.operation())) {
            return ValidationResult.rejected("Unsupported health operation: " + change.operation());
        }
        int magnitude = parsePositiveMagnitude(change);
        if (change.operation() != StateChangeOperation.SET && magnitude > maxHealthDelta) {
            return ValidationResult.rejected("Health delta exceeds max allowed per turn: " + maxHealthDelta);
        }
        return ValidationResult.accepted();
    }

    private ValidationResult validateLocation(StateChange change, String conversationCharacterId) {
        if (!conversationCharacterId.equals(change.targetId())) {
            return ValidationResult.rejected("Location changes must target the conversation character: " + conversationCharacterId);
        }
        if (!"location".equals(change.field())) {
            return ValidationResult.rejected("Location changes only support field 'location'");
        }
        if (!SET_ONLY_OPS.contains(change.operation())) {
            return ValidationResult.rejected("Location changes only support SET");
        }
        if (!LOCATION_PATTERN.matcher(change.value()).matches()) {
            return ValidationResult.rejected("Invalid location format");
        }
        return ValidationResult.accepted();
    }

    private ValidationResult validateStatus(StateChange change, String conversationCharacterId) {
        if (!conversationCharacterId.equals(change.targetId())) {
            return ValidationResult.rejected("Status changes must target the conversation character: " + conversationCharacterId);
        }
        if (!"status".equals(change.field())) {
            return ValidationResult.rejected("Status changes only support field 'status'");
        }
        if (!SET_ONLY_OPS.contains(change.operation())) {
            return ValidationResult.rejected("Status changes only support SET");
        }
        return ValidationResult.accepted();
    }

    private ValidationResult validateEmotion(StateChange change, String conversationCharacterId) {
        if (!conversationCharacterId.equals(change.targetId())) {
            return ValidationResult.rejected("Emotion changes must target the conversation character: " + conversationCharacterId);
        }
        if (!"emotion".equals(change.field())) {
            return ValidationResult.rejected("Emotion changes only support field 'emotion'");
        }
        if (!SET_ONLY_OPS.contains(change.operation())) {
            return ValidationResult.rejected("Emotion changes only support SET");
        }
        return ValidationResult.accepted();
    }

    private int parsePositiveMagnitude(StateChange change) {
        try {
            return Math.abs(Integer.parseInt(change.value().trim()));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    public record ValidationResult(boolean valid, String reason) {
        public static ValidationResult accepted() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult rejected(String reason) {
            return new ValidationResult(false, reason);
        }
    }
}
