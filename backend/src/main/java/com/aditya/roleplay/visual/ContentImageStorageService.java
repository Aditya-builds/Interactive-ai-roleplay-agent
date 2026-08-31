package com.aditya.roleplay.visual;

import com.aditya.roleplay.exception.RoleplayException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class ContentImageStorageService {

    @ConfigProperty(name = "roleplay.data.dir")
    String dataDir;

    private Path contentImagesPath;

    @PostConstruct
    void init() throws IOException {
        contentImagesPath = Path.of(dataDir).toAbsolutePath().normalize().resolve("content-images");
        Files.createDirectories(contentImagesPath.resolve("personas"));
        Files.createDirectories(contentImagesPath.resolve("characters"));
    }

    public String saveUploadedImage(String kind, byte[] bytes, String originalFilename) throws IOException {
        String normalizedKind = normalizeKind(kind);
        String extension = extensionFor(originalFilename);
        String filename = UUID.randomUUID() + extension;
        Path target = contentImagesPath.resolve(normalizedKind).resolve(filename);
        Files.write(target, bytes);
        return publicUrl(normalizedKind, filename);
    }

    public Path resolveImagePath(String kind, String filename) {
        Path resolved = contentImagesPath.resolve(normalizeKind(kind)).resolve(filename).normalize();
        if (!resolved.startsWith(contentImagesPath)) {
            throw new RoleplayException("Invalid image path", "INVALID_REQUEST", 400);
        }
        return resolved;
    }

    public String publicUrl(String kind, String filename) {
        return "/api/content/images/" + normalizeKind(kind) + "/" + filename;
    }

    private static String normalizeKind(String kind) {
        if (kind == null || kind.isBlank()) {
            return "characters";
        }
        String normalized = kind.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("personas") && !normalized.equals("characters")) {
            return "characters";
        }
        return normalized;
    }

    private static String extensionFor(String filename) {
        if (filename == null) {
            return ".jpg";
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return ".png";
        }
        if (lower.endsWith(".webp")) {
            return ".webp";
        }
        if (lower.endsWith(".gif")) {
            return ".gif";
        }
        return ".jpg";
    }
}
