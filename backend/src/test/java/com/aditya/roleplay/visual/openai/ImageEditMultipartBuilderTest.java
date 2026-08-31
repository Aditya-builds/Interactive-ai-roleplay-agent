package com.aditya.roleplay.visual.openai;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageEditMultipartBuilderTest {

    @Test
    void buildsMultipartBodyWithImageAndPromptFields() throws Exception {
        Path image = Path.of("src/test/resources").resolve("test-reference.jpg");
        if (!image.toFile().exists()) {
            image = Path.of("../data/characters/references/aurora/aurora-01.jpg");
        }

        byte[] body = ImageEditMultipartBuilder.build(
                "TestBoundary",
                List.of(image),
                ImageEditMultipartBuilder.orderedFields("gpt-image-2", "test prompt", "1536x864", true),
                "image[]");

        String content = new String(body);
        assertTrue(content.contains("Content-Disposition: form-data; name=\"image[]\""));
        assertTrue(content.contains("Content-Disposition: form-data; name=\"prompt\""));
        assertTrue(content.contains("test prompt"));
        assertTrue(content.contains("gpt-image-2"));
    }
}
