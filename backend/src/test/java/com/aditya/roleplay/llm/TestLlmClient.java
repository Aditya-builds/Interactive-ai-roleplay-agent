package com.aditya.roleplay.llm;

import com.aditya.roleplay.model.turn.StateChange;
import com.aditya.roleplay.model.turn.StateChangeOperation;
import com.aditya.roleplay.model.turn.StateChangeType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;

import java.util.List;

@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@ApplicationScoped
public class TestLlmClient implements LlmClient {

    public static volatile boolean structuredSuccess = true;

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (!structuredSuccess) {
            return new LlmResponse("plain text only", null, "test-model", 50, false);
        }
        return new LlmResponse(
                "{}",
                new LlmTurnResult(
                        "Aurora watches you carefully.",
                        List.of(new StateChange(
                                StateChangeType.RELATIONSHIP,
                                "user",
                                "familiarity",
                                StateChangeOperation.INCREASE,
                                "1")),
                        List.of(),
                        List.of()),
                "test-model",
                100,
                true);
    }
}
