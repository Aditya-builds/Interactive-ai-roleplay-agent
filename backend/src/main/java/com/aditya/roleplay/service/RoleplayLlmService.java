package com.aditya.roleplay.service;

import com.aditya.roleplay.exception.LlmException;
import com.aditya.roleplay.llm.LlmClient;
import com.aditya.roleplay.llm.LlmRequest;
import com.aditya.roleplay.llm.LlmResponse;
import com.aditya.roleplay.llm.LlmStateExtraction;
import com.aditya.roleplay.llm.LlmTurnResult;
import com.aditya.roleplay.llm.LlmTurnResultParser;
import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.PlayerPersona;
import com.aditya.roleplay.model.ReplyLength;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.Story;
import com.aditya.roleplay.model.World;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Set;

@ApplicationScoped
public class RoleplayLlmService {

    @Inject
    PromptService promptService;

    @Inject
    LlmClient llmClient;

    @Inject
    LlmTurnResultParser turnResultParser;

    @ConfigProperty(name = "roleplay.llm.two-phase", defaultValue = "false")
    boolean twoPhase;

    public LlmTurnResult generateTurnResult(
            RoleplayCharacter character,
            World world,
            Conversation conversation,
            String latestUserMessage,
            Set<String> allowedRelationshipTargets,
            PlayerPersona playerPersona,
            Story story,
            String userApiKey) {
        return generateTurnResult(
                character, world, conversation, latestUserMessage,
                allowedRelationshipTargets, playerPersona, story, userApiKey, ReplyLength.NORMAL);
    }

    public LlmTurnResult generateTurnResult(
            RoleplayCharacter character,
            World world,
            Conversation conversation,
            String latestUserMessage,
            Set<String> allowedRelationshipTargets,
            PlayerPersona playerPersona,
            Story story,
            String userApiKey,
            ReplyLength replyLength) {

        if (twoPhase) {
            return generateTwoPhaseTurnResult(
                    character, world, conversation, latestUserMessage,
                    allowedRelationshipTargets, playerPersona, story, userApiKey, replyLength);
        }
        return generateSinglePhaseTurnResult(
                character, world, conversation, latestUserMessage,
                allowedRelationshipTargets, playerPersona, story, userApiKey, replyLength);
    }

    private LlmTurnResult generateSinglePhaseTurnResult(
            RoleplayCharacter character,
            World world,
            Conversation conversation,
            String latestUserMessage,
            Set<String> allowedRelationshipTargets,
            PlayerPersona playerPersona,
            Story story,
            String userApiKey,
            ReplyLength replyLength) {

        LlmRequest request = promptService.build(
                character, world, conversation, latestUserMessage,
                allowedRelationshipTargets, playerPersona, story, replyLength);
        LlmResponse response = llmClient.complete(request, userApiKey);

        if (!response.structuredParseSuccess() || response.turnResult() == null) {
            throw new LlmException("LLM returned invalid structured output. Turn was not saved.");
        }
        return response.turnResult();
    }

    private LlmTurnResult generateTwoPhaseTurnResult(
            RoleplayCharacter character,
            World world,
            Conversation conversation,
            String latestUserMessage,
            Set<String> allowedRelationshipTargets,
            PlayerPersona playerPersona,
            Story story,
            String userApiKey,
            ReplyLength replyLength) {

        LlmRequest narrativeRequest = promptService.buildNarrativeRequest(
                character, world, conversation, latestUserMessage,
                allowedRelationshipTargets, playerPersona, story, replyLength);
        LlmResponse narrativeResponse = llmClient.complete(narrativeRequest, userApiKey);

        String narrative = resolveNarrative(narrativeResponse);

        LlmRequest extractionRequest = promptService.buildStateExtractionRequest(
                character, world, conversation, latestUserMessage, narrative,
                allowedRelationshipTargets, playerPersona, story);
        LlmResponse extractionResponse = llmClient.complete(extractionRequest, userApiKey);

        LlmStateExtraction extraction = resolveStateExtraction(extractionResponse);

        return new LlmTurnResult(
                narrative,
                extraction.stateChanges(),
                extraction.events(),
                extraction.memories());
    }

    private String resolveNarrative(LlmResponse response) {
        if (response.turnResult() != null
                && response.turnResult().response() != null
                && !response.turnResult().response().isBlank()) {
            return response.turnResult().response().trim();
        }

        LlmTurnResultParser.NarrativeParseResult parsed = turnResultParser.parseNarrative(response.rawContent());
        if (!parsed.success()) {
            throw new LlmException("LLM returned invalid narrative output. Turn was not saved.");
        }
        return parsed.narrative();
    }

    private LlmStateExtraction resolveStateExtraction(LlmResponse response) {
        if (response.turnResult() != null) {
            LlmTurnResult turnResult = response.turnResult();
            return new LlmStateExtraction(
                    turnResult.stateChanges(),
                    turnResult.events(),
                    turnResult.memories());
        }

        LlmTurnResultParser.StateExtractionParseResult parsed =
                turnResultParser.parseStateExtraction(response.rawContent());
        if (!parsed.success()) {
            throw new LlmException("LLM returned invalid state extraction output. Turn was not saved.");
        }
        return parsed.extraction();
    }
}
