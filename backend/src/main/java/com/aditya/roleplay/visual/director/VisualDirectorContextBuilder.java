package com.aditya.roleplay.visual.director;

import com.aditya.roleplay.model.CharacterRuntimeState;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.PlayerPersona;
import com.aditya.roleplay.model.Relationship;
import com.aditya.roleplay.model.Role;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Scene;
import com.aditya.roleplay.model.StoryEvent;
import com.aditya.roleplay.model.World;
import com.aditya.roleplay.model.WorldLocation;
import com.aditya.roleplay.model.visual.CharacterVisualIdentity;
import com.aditya.roleplay.model.visual.director.CandidateCharacter;
import com.aditya.roleplay.model.visual.director.CharacterStateContext;
import com.aditya.roleplay.model.visual.director.EventContext;
import com.aditya.roleplay.model.visual.director.MessageContext;
import com.aditya.roleplay.model.visual.director.RelationshipContext;
import com.aditya.roleplay.model.visual.director.SceneContext;
import com.aditya.roleplay.model.visual.director.VisualDirectorRequest;
import com.aditya.roleplay.model.visual.director.VisualIdentityContext;
import com.aditya.roleplay.service.CharacterService;
import com.aditya.roleplay.service.PlayerPersonaService;
import com.aditya.roleplay.visual.VisualIdentityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class VisualDirectorContextBuilder {

    @Inject
    CharacterService characterService;

    @Inject
    PlayerPersonaService personaService;

    @Inject
    VisualIdentityService visualIdentityService;

    @ConfigProperty(name = "roleplay.visual.director.max-messages", defaultValue = "8")
    int maxMessages;

    @ConfigProperty(name = "roleplay.visual.director.max-events", defaultValue = "5")
    int maxEvents;

    public VisualDirectorRequest build(
            Conversation conversation,
            RoleplayCharacter focalCharacter,
            World world,
            boolean explicitGeneration) {

        Scene scene = conversation.scene();
        CharacterRuntimeState state = conversation.characterState();
        Set<String> present = scene != null && scene.charactersPresent() != null
                ? new LinkedHashSet<>(scene.charactersPresent())
                : Set.of(focalCharacter.id(), "user");

        List<CandidateCharacter> candidates = new ArrayList<>();
        for (RoleplayCharacter character : characterService.listCharacters()) {
            if (!world.id().equals(character.worldId())) {
                continue;
            }
            candidates.add(new CandidateCharacter(
                    character.id(),
                    character.name(),
                    present.contains(character.id())));
        }

        List<VisualIdentityContext> identities = candidates.stream()
                .map(candidate -> toIdentityContext(
                        characterService.requireCharacter(candidate.characterId())))
                .toList();

        return new VisualDirectorRequest(
                conversation.id(),
                focalCharacter.id(),
                resolvePlayerName(conversation),
                explicitGeneration,
                toSceneContext(scene, world),
                toCharacterStateContext(focalCharacter.id(), state),
                toRecentMessages(conversation.messages()),
                toRecentEvents(conversation.events()),
                toRelationships(conversation.relationships(), focalCharacter.id()),
                candidates,
                identities);
    }

    private VisualIdentityContext toIdentityContext(RoleplayCharacter character) {
        CharacterVisualIdentity identity = visualIdentityService.resolve(character);
        return new VisualIdentityContext(
                character.id(),
                identity.canonicalReferenceImage(),
                identity.visualDescription(),
                identity.faceDescription(),
                identity.hairDescription(),
                identity.eyeDescription(),
                identity.skinDescription(),
                identity.bodyDescription(),
                identity.clothingDescription(),
                identity.accessories(),
                identity.artStyle(),
                identity.negativePrompt());
    }

    private SceneContext toSceneContext(Scene scene, World world) {
        if (scene == null) {
            return new SceneContext(null, null, null, null, null, List.of());
        }
        String description = world.locations() == null ? scene.location() : world.locations().stream()
                .filter(location -> location.id().equals(scene.location()))
                .map(WorldLocation::description)
                .findFirst()
                .orElse(scene.location());
        return new SceneContext(
                scene.location(),
                description,
                scene.time(),
                scene.currentSituation(),
                scene.currentConflict(),
                scene.charactersPresent());
    }

    private static CharacterStateContext toCharacterStateContext(String characterId, CharacterRuntimeState state) {
        if (state == null) {
            return new CharacterStateContext(characterId, null, null, null);
        }
        return new CharacterStateContext(characterId, state.emotion(), state.status(), state.location());
    }

    private List<MessageContext> toRecentMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<Message> dialogue = messages.stream()
                .filter(message -> !message.isSceneImage())
                .filter(message -> message.content() != null && !message.content().isBlank())
                .toList();
        int start = Math.max(0, dialogue.size() - maxMessages);
        return dialogue.subList(start, dialogue.size()).stream()
                .map(message -> new MessageContext(
                        message.id(),
                        message.role() == Role.USER ? "user" : "assistant",
                        message.content()))
                .toList();
    }

    private List<EventContext> toRecentEvents(List<StoryEvent> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        int start = Math.max(0, events.size() - maxEvents);
        return events.subList(start, events.size()).stream()
                .map(event -> new EventContext(event.description(), event.importance()))
                .toList();
    }

    private static List<RelationshipContext> toRelationships(List<Relationship> relationships, String sourceId) {
        if (relationships == null) {
            return List.of();
        }
        return relationships.stream()
                .map(relationship -> new RelationshipContext(
                        sourceId,
                        relationship.targetId(),
                        relationship.trust(),
                        relationship.respect(),
                        relationship.affection(),
                        relationship.familiarity(),
                        relationship.suspicion()))
                .toList();
    }

    private String resolvePlayerName(Conversation conversation) {
        if (conversation.playerPersonaId() == null || conversation.playerPersonaId().isBlank()) {
            return "the player";
        }
        try {
            PlayerPersona persona = personaService.requirePersona(conversation.playerPersonaId());
            if (persona.name() != null && !persona.name().isBlank()) {
                return persona.name();
            }
        } catch (RuntimeException ignored) {
            // Fall back to generic label.
        }
        return "the player";
    }
}
