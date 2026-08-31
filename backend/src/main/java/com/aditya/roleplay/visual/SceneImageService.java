package com.aditya.roleplay.visual;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.Message;
import com.aditya.roleplay.model.Role;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.World;
import com.aditya.roleplay.model.visual.GenerateSceneImageResponse;
import com.aditya.roleplay.model.visual.GeneratedSceneImage;
import com.aditya.roleplay.model.visual.VisualSceneState;
import com.aditya.roleplay.service.CharacterService;
import com.aditya.roleplay.service.ConversationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SceneImageService {

    @Inject
    ConversationService conversationService;

    @Inject
    CharacterService characterService;

    @Inject
    VisualScenePlannerService scenePlannerService;

    @Inject
    VisualPromptService visualPromptService;

    @Inject
    VisualIdentityService visualIdentityService;

    @Inject
    VisualImageStorageService imageStorageService;

    @Inject
    ImageGenerationClient imageGenerationClient;

    @ConfigProperty(name = "roleplay.visual.enabled", defaultValue = "true")
    boolean visualEnabled;

    @ConfigProperty(name = "roleplay.visual.default-width", defaultValue = "1024")
    int defaultWidth;

    @ConfigProperty(name = "roleplay.visual.default-height", defaultValue = "576")
    int defaultHeight;

    @ConfigProperty(name = "roleplay.visual.default-aspect-ratio", defaultValue = "16:9")
    String defaultAspectRatio;

    @ConfigProperty(name = "roleplay.visual.model", defaultValue = "gpt-image-2")
    String defaultModel;

    public GenerateSceneImageResponse generateForConversation(String conversationId) {
        return generateForConversation(conversationId, null);
    }

    public GenerateSceneImageResponse generateForConversation(String conversationId, String userApiKey) {
        if (!visualEnabled) {
            throw new RoleplayException("Visual generation is disabled.", "VISUAL_DISABLED", 503);
        }

        Conversation conversation = conversationService.getConversation(conversationId);
        RoleplayCharacter character = characterService.requireCharacter(conversation.characterId());
        World world = characterService.requireWorld(conversation.worldId());

        if (visualIdentityService.resolveReferenceImagePaths(character, imageStorageService).isEmpty()) {
            throw new RoleplayException(
                    "No canonical reference image configured for character: " + character.id(),
                    "VISUAL_REFERENCE_NOT_FOUND",
                    404);
        }

        VisualSceneState sceneState = scenePlannerService.plan(conversation, character, world);
        ImageGenerationRequest generationRequest = visualPromptService.buildRequest(
                character, sceneState, defaultWidth, defaultHeight, defaultAspectRatio, defaultModel, imageStorageService);

        ImageGenerationResponse generationResponse = imageGenerationClient.generate(generationRequest, userApiKey);

        String imageId = UUID.randomUUID().toString();
        String imageUrl = imageStorageService.publicImageUrl(imageId);
        String sourceMessageId = latestMessageId(conversation);

        GeneratedSceneImage metadata = new GeneratedSceneImage(
                imageId,
                conversationId,
                List.of(character.id()),
                sourceMessageId,
                generationRequest.prompt(),
                generationRequest.negativePrompt(),
                generationResponse.provider(),
                generationResponse.model(),
                imageUrl,
                Instant.now());

        try {
            imageStorageService.saveGeneratedImage(
                    imageId, generationResponse.imageBytes(), generationResponse.mimeType(), metadata);
        } catch (IOException e) {
            throw new RoleplayException(
                    "Failed to store generated scene image: " + e.getMessage(),
                    "STORAGE_ERROR",
                    500);
        }

        String caption = buildCaption(character.name(), sceneState);
        Message sceneImageMessage = new Message(
                UUID.randomUUID().toString(),
                Role.ASSISTANT,
                caption,
                Instant.now(),
                imageId);

        Conversation updated = conversation
                .appendMessage(sceneImageMessage)
                .withUpdatedAt(Instant.now());
        conversationService.save(updated);

        return new GenerateSceneImageResponse(metadata, sceneImageMessage);
    }

    public GeneratedSceneImage getSceneImage(String imageId) {
        return imageStorageService.loadMetadata(imageId)
                .orElseThrow(() -> new RoleplayException(
                        "Scene image not found: " + imageId, "SCENE_IMAGE_NOT_FOUND", 404));
    }

    private static String latestMessageId(Conversation conversation) {
        if (conversation.messages().isEmpty()) {
            return null;
        }
        return conversation.messages().get(conversation.messages().size() - 1).id();
    }

    private static String buildCaption(String characterName, VisualSceneState sceneState) {
        return "Scene: %s at %s".formatted(characterName, sceneState.location());
    }
}
