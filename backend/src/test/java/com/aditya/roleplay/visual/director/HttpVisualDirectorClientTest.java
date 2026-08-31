package com.aditya.roleplay.visual.director;

import com.aditya.roleplay.model.visual.director.VisualDirectorRequest;
import com.aditya.roleplay.model.visual.director.VisualScenePlan;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class HttpVisualDirectorClientTest {

    @Inject
    HttpVisualDirectorClient client;

    @Test
    void returnsEmptyWhenDirectorDisabled() {
        Optional<VisualScenePlan> plan = client.plan(new VisualDirectorRequest(
                "conv",
                "aurora",
                "Player",
                true,
                null,
                null,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of()));

        assertTrue(plan.isEmpty());
    }
}
