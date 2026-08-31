package com.aditya.roleplay.visual.director;

import com.aditya.roleplay.model.visual.director.VisualDirectorRequest;
import com.aditya.roleplay.model.visual.director.VisualDirectorResponse;
import com.aditya.roleplay.model.visual.director.VisualScenePlan;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@ApplicationScoped
public class HttpVisualDirectorClient implements VisualDirectorClient {

    private static final Logger LOG = Logger.getLogger(HttpVisualDirectorClient.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ConfigProperty(name = "roleplay.visual.director.enabled", defaultValue = "false")
    boolean directorEnabled;

    @ConfigProperty(name = "roleplay.visual.director.base-url", defaultValue = "http://localhost:8090")
    String directorBaseUrl;

    @ConfigProperty(name = "roleplay.visual.director.timeout-ms", defaultValue = "30000")
    long timeoutMs;

    @Override
    public Optional<VisualScenePlan> plan(VisualDirectorRequest request) {
        if (!directorEnabled) {
            return Optional.empty();
        }

        String baseUrl = directorBaseUrl.endsWith("/")
                ? directorBaseUrl.substring(0, directorBaseUrl.length() - 1)
                : directorBaseUrl;

        try {
            String body = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/visual/plan"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.warnf(
                        "Visual director returned status %d for conversation %s",
                        response.statusCode(),
                        request.conversationId());
                return Optional.empty();
            }

            VisualDirectorResponse parsed = objectMapper.readValue(response.body(), VisualDirectorResponse.class);
            if (parsed.plan() == null) {
                return Optional.empty();
            }
            return Optional.of(parsed.plan());
        } catch (Exception e) {
            LOG.warnf(
                    e,
                    "Visual director unavailable for conversation %s; falling back to V1 planner",
                    request.conversationId());
            return Optional.empty();
        }
    }
}
