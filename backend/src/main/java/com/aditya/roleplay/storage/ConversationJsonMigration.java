package com.aditya.roleplay.storage;

import com.aditya.roleplay.model.Relationship;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class ConversationJsonMigration {

    private ConversationJsonMigration() {
    }

    static JsonNode migrate(ObjectMapper mapper, JsonNode root) {
        if (!root.isObject()) {
            return root;
        }
        ObjectNode object = (ObjectNode) root;

        if (object.has("relationship") && !object.has("relationships")) {
            JsonNode legacy = object.get("relationship");
            ArrayNode relationships = mapper.createArrayNode();
            ObjectNode entry = mapper.createObjectNode();
            String characterId = object.path("characterId").asText("");
            String targetId = legacy.path("targetId").asText("");
            if (targetId.isBlank() || characterId.equals(targetId)) {
                targetId = "user";
            }
            entry.put("targetId", targetId);
            entry.put("trust", legacy.path("trust").asInt(40));
            entry.put("respect", legacy.path("respect").asInt(50));
            entry.put("affection", legacy.path("affection").asInt(10));
            entry.put("familiarity", legacy.path("familiarity").asInt(20));
            entry.put("suspicion", legacy.path("suspicion").asInt(5));
            relationships.add(entry);
            object.set("relationships", relationships);
            object.remove("relationship");
        }

        if (object.has("characterState") && object.get("characterState").isObject()
                && object.has("scene") && object.get("scene").isObject()) {
            ObjectNode characterState = (ObjectNode) object.get("characterState");
            ObjectNode scene = (ObjectNode) object.get("scene");
            String sceneLocation = scene.path("location").asText("");
            if (!sceneLocation.isBlank()
                    && (!characterState.has("location") || characterState.get("location").asText("").isBlank())) {
                characterState.put("location", sceneLocation);
            }
            if (!scene.has("userLocation") || scene.get("userLocation").asText("").isBlank()) {
                scene.put("userLocation", sceneLocation);
            }
        }

        return object;
    }
}
