package com.aditya.roleplay.llm;

import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import com.aditya.roleplay.model.turn.StateChangeType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@ApplicationScoped
public class TestLlmClient implements LlmClient {

    public static volatile boolean structuredSuccess = true;

    private static final Deque<LlmTurnResult> SCRIPTED_RESULTS = new ArrayDeque<>();

    public static void reset() {
        structuredSuccess = true;
        SCRIPTED_RESULTS.clear();
    }

    public static void enqueue(LlmTurnResult result) {
        SCRIPTED_RESULTS.addLast(result);
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        return complete(request, null);
    }

    @Override
    public LlmResponse complete(LlmRequest request, String apiKeyOverride) {
        if (!structuredSuccess) {
            return new LlmResponse("plain text only", null, "test-model", 50, false);
        }

        LlmTurnResult scripted = SCRIPTED_RESULTS.pollFirst();
        LlmTurnResult turnResult = scripted != null
                ? adaptToKind(scripted, request.kind())
                : defaultResult(request.kind());

        return new LlmResponse("{}", turnResult, "test-model", 100, true);
    }

    private static LlmTurnResult adaptToKind(LlmTurnResult result, LlmRequestKind kind) {
        return switch (kind) {
            case NARRATIVE_ONLY -> new LlmTurnResult(
                    result.response(), List.of(), List.of(), List.of());
            case STATE_EXTRACTION -> new LlmTurnResult(
                    "", result.stateChanges(), result.events(), result.memories());
            case FULL_TURN -> result;
        };
    }

    private static LlmTurnResult defaultResult(LlmRequestKind kind) {
        List<StateChange> defaultStateChange = List.of(new StateChange(
                StateChangeType.RELATIONSHIP,
                "user",
                "familiarity",
                StateChangeOperation.INCREASE,
                "1"));

        return switch (kind) {
            case NARRATIVE_ONLY -> new LlmTurnResult(
                    "Aurora watches you carefully.", List.of(), List.of(), List.of());
            case STATE_EXTRACTION -> new LlmTurnResult(
                    "", defaultStateChange, List.of(), List.of());
            case FULL_TURN -> new LlmTurnResult(
                    "Aurora watches you carefully.", defaultStateChange, List.of(), List.of());
        };
    }
}
