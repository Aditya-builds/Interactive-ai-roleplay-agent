package com.aditya.roleplay.visual;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.visual.GeneratedSceneImage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VisualImageStorageService {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @ConfigProperty(name = "roleplay.data.dir")
    String dataDir;

    private Path generatedImagesPath;
    private Path referencesPath;

    @PostConstruct
    void init() throws IOException {
        Path dataPath = Path.of(dataDir).toAbsolutePath().normalize();
        generatedImagesPath = dataPath.resolve("generated-images");
        referencesPath = dataPath.resolve("characters").resolve("references");
        Files.createDirectories(generatedImagesPath);
        Files.createDirectories(referencesPath);
    }

    public Path canonicalReferencePath(String characterId) {
        Path canonical = referencesPath.resolve(characterId + "-canonical.jpg");
        if (Files.exists(canonical)) {
            return canonical;
        }
        Path png = referencesPath.resolve(characterId + "-canonical.png");
        if (Files.exists(png)) {
            return png;
        }
        return canonical;
    }

    public boolean canonicalReferenceExists(String characterId) {
        return Files.exists(canonicalReferencePath(characterId));
    }

    public void saveGeneratedImage(String imageId, byte[] bytes, String mimeType, GeneratedSceneImage metadata)
            throws IOException {
        String extension = extensionForMime(mimeType);
        Path imagePath = generatedImagesPath.resolve(imageId + extension);
        Path metadataPath = generatedImagesPath.resolve(imageId + ".json");

        Path tempImage = generatedImagesPath.resolve(imageId + extension + ".tmp");
        Files.write(tempImage, bytes);
        Files.move(tempImage, imagePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        objectMapper.writeValue(metadataPath.toFile(), metadata);
    }

    public Optional<GeneratedSceneImage> loadMetadata(String imageId) {
        Path metadataPath = generatedImagesPath.resolve(imageId + ".json");
        if (!Files.exists(metadataPath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(metadataPath.toFile(), GeneratedSceneImage.class));
        } catch (IOException e) {
            throw new RoleplayException("Failed to load scene image metadata: " + imageId, "STORAGE_ERROR", 500);
        }
    }

    public Optional<Path> loadImageFile(String imageId) {
        for (String extension : List.of(".jpg", ".jpeg", ".png")) {
            Path imagePath = generatedImagesPath.resolve(imageId + extension);
            if (Files.exists(imagePath)) {
                return Optional.of(imagePath);
            }
        }
        return Optional.empty();
    }

    public String publicImageUrl(String imageId) {
        return "/api/scene-images/" + imageId + "/content";
    }

    private static String extensionForMime(String mimeType) {
        if (mimeType != null && mimeType.contains("png")) {
            return ".png";
        }
        return ".jpg";
    }
}
