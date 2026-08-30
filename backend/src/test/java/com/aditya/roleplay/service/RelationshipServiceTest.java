package com.aditya.roleplay.service;

import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import com.aditya.roleplay.model.turn.StateChangeType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelationshipServiceTest {

    private final RelationshipService relationshipService = new RelationshipService();

    @Test
    void increasesRelationshipValue() {
        Relationship initial = new Relationship("user", 40, 50, 10, 20, 5);

        Relationship updated = relationshipService.applyStateChange(List.of(initial), new StateChange(
                StateChangeType.RELATIONSHIP,
                "user",
                "trust",
                StateChangeOperation.INCREASE,
                "5"), "aurora").get(0);

        assertEquals(45, updated.trust());
    }

    @Test
    void decreasesRelationshipValue() {
        Relationship initial = new Relationship("user", 40, 50, 10, 20, 5);

        Relationship updated = relationshipService.applyStateChange(List.of(initial), new StateChange(
                StateChangeType.RELATIONSHIP,
                "user",
                "respect",
                StateChangeOperation.DECREASE,
                "3"), "aurora").get(0);

        assertEquals(47, updated.respect());
    }

    @Test
    void clampsRelationshipAt100() {
        Relationship initial = new Relationship("user", 98, 100, 100, 100, 0);

        Relationship updated = relationshipService.applyStateChange(List.of(initial), new StateChange(
                StateChangeType.RELATIONSHIP,
                "user",
                "trust",
                StateChangeOperation.INCREASE,
                "10"), "aurora").get(0);

        assertEquals(100, updated.trust());
    }

    @Test
    void clampsRelationshipAtZero() {
        Relationship initial = new Relationship("user", 2, 50, 10, 20, 5);

        Relationship updated = relationshipService.applyStateChange(List.of(initial), new StateChange(
                StateChangeType.RELATIONSHIP,
                "user",
                "suspicion",
                StateChangeOperation.DECREASE,
                "10"), "aurora").get(0);

        assertEquals(0, updated.suspicion());
    }

    @Test
    void createsRelationshipForNewTarget() {
        Relationship updated = relationshipService.applyStateChange(List.of(), new StateChange(
                StateChangeType.RELATIONSHIP,
                "laxus",
                "trust",
                StateChangeOperation.INCREASE,
                "4"), "aurora").get(0);

        assertEquals("laxus", updated.targetId());
        assertEquals(44, updated.trust());
    }
}
