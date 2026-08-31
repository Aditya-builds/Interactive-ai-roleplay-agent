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
import com.aditya.roleplay.model.visual.director.VisualScenePlan;
import com.aditya.roleplay.service.CharacterService;
import com.aditya.roleplay.service.ConversationService;
import com.aditya.roleplay.visual.director.VisualDirectorClient;
import com.aditya.roleplay.visual.director.VisualDirectorContextBuilder;
import com.aditya.roleplay.visual.director.VisualPromptCompilerService;
import com.aditya.roleplay.visual.reference.CharacterReferenceLibraryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SceneImageService {

    private static final Logger LOG = Logger.getLogger(SceneImageService.class);

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

    @Inject
    VisualDirectorClient visualDirectorClient;

    @Inject
    VisualDirectorContextBuilder visualDirectorContextBuilder;

    @Inject
    VisualPromptCompilerService visualPromptCompilerService;

    @Inject
    CharacterReferenceLibraryService referenceLibraryService;

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

        if (!visualIdentityService.hasResolvableReferences(character, imageStorageService)
                && !referenceLibraryService.hasLibrary(character.id())) {
            throw new RoleplayException(
                    "No canonical reference image configured for character: " + character.id(),
                    "VISUAL_REFERENCE_NOT_FOUND",
                    404);
        }

        VisualSceneState fallbackSceneState = scenePlannerService.plan(conversation, character, world);
        ImageGenerationRequest generationRequest = null;
        List<String> characterIds = new ArrayList<>();
        String captionLocation = fallbackSceneState.location();
        String plannerVersion = "v1";
        long directorMs = 0;

        Optional<VisualScenePlan> directorPlan = tryDirectorPlan(conversation, character, world);
        if (directorPlan.isPresent()) {
            VisualScenePlan plan = directorPlan.get();
            directorMs = plan.graphExecutionMs();
            LOG.infof(
                    "visual_director_plan conversationId=%s shouldGenerate=%s momentType=%s characters=%d graphExecutionMs=%d",
                    conversationId,
                    plan.shouldGenerate(),
                    plan.momentType(),
                    plan.characters().size(),
                    directorMs);

            if (plan.shouldGenerate() && plan.prompt() != null && !plan.prompt().isBlank()) {
                generationRequest = visualPromptCompilerService.compile(
                        plan,
                        character,
                        defaultWidth,
                        defaultHeight,
                        defaultAspectRatio,
                        defaultModel,
                        imageStorageService);
                characterIds.addAll(plan.characters().stream()
                        .map(com.aditya.roleplay.model.visual.director.VisualPlanCharacter::characterId)
                        .filter(id -> !"user".equals(id) && !"player".equals(id))
                        .toList());
                if (characterIds.isEmpty()) {
                    characterIds.add(character.id());
                }
                captionLocation = plan.scene() != null && plan.scene().location() != null
                        ? plan.scene().location()
                        : captionLocation;
                plannerVersion = "v2";
            }
        }

        if (generationRequest == null) {
            LOG.infof("visual_generation_fallback conversationId=%s planner=v1", conversationId);
            generationRequest = visualPromptService.buildRequest(
                    character, fallbackSceneState, defaultWidth, defaultHeight, defaultAspectRatio, defaultModel, imageStorageService);
            characterIds = List.of(character.id());
            captionLocation = fallbackSceneState.location();
        }

        long imageStart = System.nanoTime();
        ImageGenerationResponse generationResponse = imageGenerationClient.generate(generationRequest, userApiKey);
        long imageMs = (System.nanoTime() - imageStart) / 1_000_000;

        String imageId = UUID.randomUUID().toString();
        String imageUrl = imageStorageService.publicImageUrl(imageId);
        String sourceMessageId = latestMessageId(conversation);

        GeneratedSceneImage metadata = new GeneratedSceneImage(
                imageId,
                conversationId,
                characterIds,
                sourceMessageId,
                generationRequest.prompt(),
                generationRequest.negativePrompt(),
                generationResponse.provider(),
                generationResponse.model(),
                imageUrl,
                Instant.now(),
                generationRequest.selectedReferenceIds(),
                generationRequest.selectedReferenceIds().isEmpty()
                        ? generationRequest.referenceImagePaths().size()
                        : generationRequest.selectedReferenceIds().size(),
                generationRequest.referenceSelectionSummary());

        try {
            imageStorageService.saveGeneratedImage(
                    imageId, generationResponse.imageBytes(), generationResponse.mimeType(), metadata);
        } catch (IOException e) {
            throw new RoleplayException(
                    "Failed to store generated scene image: " + e.getMessage(),
                    "STORAGE_ERROR",
                    500);
        }

        LOG.infof(
                "visual_generation_complete conversationId=%s planner=%s provider=%s model=%s promptLength=%d referenceCount=%d referenceIds=%s directorMs=%d imageMs=%d success=true",
                conversationId,
                plannerVersion,
                generationResponse.provider(),
                generationResponse.model(),
                generationRequest.prompt().length(),
                generationRequest.referenceImagePaths().size(),
                generationRequest.selectedReferenceIds(),
                directorMs,
                imageMs);

        String caption = buildCaption(character.name(), captionLocation);
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

    private Optional<VisualScenePlan> tryDirectorPlan(
            Conversation conversation,
            RoleplayCharacter character,
            World world) {
        try {
            var request = visualDirectorContextBuilder.build(conversation, character, world, true);
            return visualDirectorClient.plan(request);
        } catch (Exception e) {
            LOG.warnf(e, "visual_director_failed conversationId=%s", conversation.id());
            return Optional.empty();
        }
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

    private static String buildCaption(String characterName, String location) {
        return "Scene: %s at %s".formatted(characterName, location);
    }
}
