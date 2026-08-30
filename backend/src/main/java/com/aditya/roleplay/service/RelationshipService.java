package com.aditya.roleplay.service;

import com.aditya.roleplay.model.Relationship;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RelationshipService {

    public Relationship createInitialRelationship(String characterId) {
        return new Relationship(characterId, 42, 67, 12, 54, 8);
    }

    public Relationship applyPostTurnUpdates(Relationship relationship, String userMessage) {
        int trust = relationship.trust();
        int respect = relationship.respect();
        int affection = relationship.affection();
        int familiarity = relationship.familiarity() + 1;
        int suspicion = relationship.suspicion();

        String lower = userMessage.toLowerCase();
        if (containsAny(lower, "help", "protect", "save", "defend")) {
            trust += 2;
            respect += 1;
        }
        if (containsAny(lower, "thank", "grateful", "appreciate")) {
            affection += 1;
            respect += 1;
        }
        if (containsAny(lower, "lie", "betray", "deceive", "steal")) {
            trust -= 3;
            suspicion += 2;
        }

        return new Relationship(
                relationship.characterId(),
                trust,
                respect,
                affection,
                familiarity,
                suspicion).clamped();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
