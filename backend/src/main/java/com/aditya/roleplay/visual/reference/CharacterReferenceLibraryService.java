package com.aditya.roleplay.visual.reference;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.visual.reference.CharacterReferenceImage;
import com.aditya.roleplay.model.visual.reference.CharacterReferenceImageSummary;
import com.aditya.roleplay.model.visual.reference.CharacterReferenceLibrary;
import com.aditya.roleplay.model.visual.reference.CharacterReferenceLibrarySummary;
import com.aditya.roleplay.model.visual.reference.ReferenceImageType;
import com.aditya.roleplay.visual.VisualImageStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads per-character reference libraries from data-driven JSON files.
 * Convention: {@code data/characters/{characterId}.references.json}
 */
@ApplicationScoped
public class CharacterReferenceLibraryService {

    private static final Logger LOG = Logger.getLogger(CharacterReferenceLibraryService.class);

    @Inject
    VisualImageStorageService imageStorageService;

    @ConfigProperty(name = "roleplay.data.dir")
    String dataDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Optional<CharacterReferenceLibrary>> cache = new ConcurrentHashMap<>();
    private Path charactersPath;
    private Path dataPath;

    @PostConstruct
    void init() {
        dataPath = Path.of(dataDir).toAbsolutePath().normalize();
        charactersPath = dataPath.resolve("characters");
    }

    public Optional<CharacterReferenceLibrary> findLibrary(String characterId) {
        return cache.computeIfAbsent(characterId, id -> Optional.ofNullable(loadLibrary(id)));
    }

    public boolean hasLibrary(String characterId) {
        return findLibrary(characterId).isPresent();
    }

    public CharacterReferenceLibrarySummary summarize(String characterId) {
        CharacterReferenceLibrary library = findLibrary(characterId)
                .orElseThrow(() -> new RoleplayException(
                        "Reference library not found for character: " + characterId,
                        "REFERENCE_LIBRARY_NOT_FOUND",
                        404));

        return new CharacterReferenceLibrarySummary(
                library.characterId(),
                library.canonicalReferenceId(),
                library.images().size(),
                library.images().stream()
                        .map(image -> toSummary(characterId, library, image))
                        .toList());
    }

    public Path resolveFilesystemPath(CharacterReferenceLibrary library, CharacterReferenceImage image) {
        if (library.directory() != null && !library.directory().isBlank()) {
            Path resolved = dataPath.resolve(library.directory()).resolve(image.filename()).normalize();
            if (Files.exists(resolved)) {
                return resolved;
            }
            LOG.warnf("reference file missing at %s", resolved);
        }
        return imageStorageService.characterReferenceImagePath(library.characterId(), image.filename())
                .toAbsolutePath()
                .normalize();
    }

    public List<String> resolveExistingPaths(CharacterReferenceLibrary library, List<CharacterReferenceImage> images) {
        return images.stream()
                .map(image -> resolveFilesystemPath(library, image))
                .filter(Files::exists)
                .map(path -> path.toAbsolutePath().toString())
                .distinct()
                .toList();
    }

    public String publicImageUrl(String characterId, String referenceId) {
        return "/api/visuals/references/" + characterId + "/images/" + referenceId;
    }

    private CharacterReferenceImageSummary toSummary(
            String characterId,
            CharacterReferenceLibrary library,
            CharacterReferenceImage image) {
        return new CharacterReferenceImageSummary(
                image.id(),
                image.imageType().name().toLowerCase(),
                image.pose().name().toLowerCase(),
                image.cameraAngle().name().toLowerCase(),
                image.expression().name().toLowerCase(),
                image.clothing().name().toLowerCase(),
                image.action().name().toLowerCase(),
                image.framing().name().toLowerCase(),
                image.notes(),
                image.priority(),
                image.identityAnchor(),
                image.id().equals(library.canonicalReferenceId())
                        || image.imageType() == ReferenceImageType.CANONICAL,
                publicImageUrl(characterId, image.id()));
    }

    private CharacterReferenceLibrary loadLibrary(String characterId) {
        Path libraryFile = charactersPath.resolve(characterId + ".references.json");
        if (!Files.exists(libraryFile)) {
            return null;
        }
        try {
            CharacterReferenceLibrary library = objectMapper.readValue(libraryFile.toFile(), CharacterReferenceLibrary.class);
            if (!characterId.equals(library.characterId())) {
                throw new RoleplayException(
                        "Reference library characterId mismatch for " + characterId,
                        "REFERENCE_LIBRARY_INVALID",
                        500);
            }
            return library;
        } catch (IOException e) {
            throw new RoleplayException(
                    "Failed to load reference library for " + characterId + ": " + e.getMessage(),
                    "REFERENCE_LIBRARY_INVALID",
                    500);
        }
    }
}
