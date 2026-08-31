package com.aditya.roleplay.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmTurnResultParserTest {

    private final LlmTurnResultParser parser = new LlmTurnResultParser();

    @Test
    void parsesValidStructuredResponse() {
        String json = """
                {
                  "response": "Laxus grins faintly.",
                  "stateChanges": [
                    {
                      "type": "RELATIONSHIP",
                      "targetId": "aurora",
                      "field": "respect",
                      "operation": "INCREASE",
                      "value": "2"
                    }
                  ],
                  "events": [],
                  "memories": []
                }
                """;

        LlmTurnResultParser.ParseResult result = parser.parse(json);

        assertTrue(result.success());
        assertEquals("Laxus grins faintly.", result.turnResult().response());
        assertEquals(1, result.turnResult().stateChanges().size());
    }

    @Test
    void parsesJsonInsideMarkdownCodeBlock() {
        String wrapped = """
                ```json
                {"response":"Hello.","stateChanges":[],"events":[],"memories":[]}
                ```
                """;

        LlmTurnResultParser.ParseResult result = parser.parse(wrapped);

        assertTrue(result.success());
        assertEquals("Hello.", result.turnResult().response());
    }

    @Test
    void rejectsMissingNarrativeResponse() {
        String json = """
                {"response":"","stateChanges":[],"events":[],"memories":[]}
                """;

        LlmTurnResultParser.ParseResult result = parser.parse(json);

        assertFalse(result.success());
    }

    @Test
    void rejectsMalformedJson() {
        LlmTurnResultParser.ParseResult result = parser.parse("not json");

        assertFalse(result.success());
    }

    @Test
    void parsesNarrativeOnlyResponse() {
        String json = """
                {"response": "Runa nods slowly."}
                """;

        LlmTurnResultParser.NarrativeParseResult result = parser.parseNarrative(json);

        assertTrue(result.success());
        assertEquals("Runa nods slowly.", result.narrative());
    }

    @Test
    void parsesStateExtractionResponse() {
        String json = """
                {
                  "stateChanges": [
                    {
                      "type": "RELATIONSHIP",
                      "targetId": "user",
                      "field": "familiarity",
                      "operation": "INCREASE",
                      "value": "1"
                    }
                  ],
                  "events": [],
                  "memories": []
                }
                """;

        LlmTurnResultParser.StateExtractionParseResult result = parser.parseStateExtraction(json);

        assertTrue(result.success());
        assertEquals(1, result.extraction().stateChanges().size());
        assertEquals("user", result.extraction().stateChanges().get(0).targetId());
    }
}
