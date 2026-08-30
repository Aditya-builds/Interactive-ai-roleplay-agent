package com.aditya.roleplay.service;

import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import com.aditya.roleplay.model.turn.StateChangeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelationshipServiceTest {

    private final RelationshipService relationshipService = new RelationshipService();

    @Test
    void increasesRelationshipValue() {
        Relationship initial = new Relationship("aurora", 40, 50, 10, 20, 5);

        Relationship updated = relationshipService.applyStateChange(initial, new StateChange(
                StateChangeType.RELATIONSHIP,
                "aurora",
                "trust",
                StateChangeOperation.INCREASE,
                "5"));

        assertEquals(45, updated.trust());
    }

    @Test
    void decreasesRelationshipValue() {
        Relationship initial = new Relationship("aurora", 40, 50, 10, 20, 5);

        Relationship updated = relationshipService.applyStateChange(initial, new StateChange(
                StateChangeType.RELATIONSHIP,
                "aurora",
                "respect",
                StateChangeOperation.DECREASE,
                "3"));

        assertEquals(47, updated.respect());
    }

    @Test
    void clampsRelationshipAt100() {
        Relationship initial = new Relationship("aurora", 98, 100, 100, 100, 0);

        Relationship updated = relationshipService.applyStateChange(initial, new StateChange(
                StateChangeType.RELATIONSHIP,
                "aurora",
                "trust",
                StateChangeOperation.INCREASE,
                "10"));

        assertEquals(100, updated.trust());
    }

    @Test
    void clampsRelationshipAtZero() {
        Relationship initial = new Relationship("aurora", 2, 50, 10, 20, 5);

        Relationship updated = relationshipService.applyStateChange(initial, new StateChange(
                StateChangeType.RELATIONSHIP,
                "aurora",
                "suspicion",
                StateChangeOperation.DECREASE,
                "10"));

        assertEquals(0, updated.suspicion());
    }
}
