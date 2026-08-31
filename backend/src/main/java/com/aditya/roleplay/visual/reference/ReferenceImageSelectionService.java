package com.aditya.roleplay.visual.reference;

import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.model.visual.CharacterVisualIdentity;
import com.aditya.roleplay.model.visual.VisualCharacterScenePresence;
import com.aditya.roleplay.model.visual.VisualSceneState;
import com.aditya.roleplay.model.visual.reference.CharacterReferenceImage;
import com.aditya.roleplay.model.visual.reference.CharacterReferenceLibrary;
import com.aditya.roleplay.model.visual.reference.ReferenceAction;
import com.aditya.roleplay.model.visual.reference.ReferenceCameraAngle;
import com.aditya.roleplay.model.visual.reference.ReferenceClothing;
import com.aditya.roleplay.model.visual.reference.ReferenceExpression;
import com.aditya.roleplay.model.visual.reference.ReferenceFraming;
import com.aditya.roleplay.model.visual.reference.ReferenceImageType;
import com.aditya.roleplay.model.visual.reference.ReferencePose;
import com.aditya.roleplay.model.visual.reference.ReferenceSelectionResult;
import com.aditya.roleplay.model.visual.reference.SceneReferenceHints;
import com.aditya.roleplay.visual.VisualIdentityService;
import com.aditya.roleplay.visual.VisualImageStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * Deterministic reference-image selection using metadata scoring and diversity constraints.
 */
@ApplicationScoped
public class ReferenceImageSelectionService {

    private static final Logger LOG = Logger.getLogger(ReferenceImageSelectionService.class);

    private static final int SCORE_CANONICAL_IDENTITY = 100;
    private static final int SCORE_FACE_IDENTITY = 40;
    private static final int SCORE_CAMERA_MATCH = 25;
    private static final int SCORE_FRAMING_MATCH = 20;
    private static final int SCORE_POSE_MATCH = 25;
    private static final int SCORE_EXPRESSION_MATCH = 15;
    private static final int SCORE_CLOTHING_MATCH = 15;
    private static final int SCORE_ACTION_MATCH = 20;
    private static final int SCORE_DIVERSITY_BONUS = 10;

    @Inject
    CharacterReferenceLibraryService libraryService;

    @Inject
    SceneReferenceHintsExtractor hintsExtractor;

    @Inject
    VisualIdentityService visualIdentityService;

    @ConfigProperty(name = "roleplay.visual.references.max-per-request", defaultValue = "5")
    int maxPerRequest;

    @ConfigProperty(name = "roleplay.visual.references.min-per-request", defaultValue = "3")
    int minPerRequest;

    public ReferenceSelectionResult select(
            RoleplayCharacter character,
            CharacterVisualIdentity identity,
            VisualSceneState sceneState,
            VisualCharacterScenePresence presence,
            VisualImageStorageService storage) {

        Optional<CharacterReferenceLibrary> library = libraryService.findLibrary(character.id());
        if (library.isEmpty()) {
            LOG.infof("reference_selection_fallback character=%s reason=no_library", character.id());
            return legacyFallback(character, storage);
        }

        SceneReferenceHints hints = hintsExtractor.extract(sceneState, presence);
        List<ScoredReference> scored = scoreLibrary(library.get(), hints);

        List<CharacterReferenceImage> selected = pickDiverseReferences(scored, library.get());
        selected = ensureIdentityAnchors(selected, library.get());
        List<String> paths = libraryService.resolveExistingPaths(library.get(), selected);

        if (paths.isEmpty()) {
            LOG.warnf(
                    "reference_selection_paths_missing character=%s libraryImages=%d selected=%d",
                    character.id(),
                    library.get().images().size(),
                    selected.size());
            selected = pickIdentityAnchors(library.get());
            paths = libraryService.resolveExistingPaths(library.get(), selected);
        }

        if (paths.isEmpty()) {
            LOG.warnf("reference_selection_fallback character=%s reason=no_paths_on_disk", character.id());
            return legacyFallback(character, storage);
        }

        String summary = selected.stream()
                .map(image -> image.id() + "(" + image.imageType().name().toLowerCase() + ")")
                .collect(Collectors.joining(", "));

        LOG.infof(
                "reference_selection character=%s selected=%d paths=%d summary=%s",
                character.id(),
                selected.size(),
                paths.size(),
                summary);

        return new ReferenceSelectionResult(selected, paths, summary);
    }

    private List<CharacterReferenceImage> ensureIdentityAnchors(
            List<CharacterReferenceImage> selected,
            CharacterReferenceLibrary library) {
        List<CharacterReferenceImage> merged = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        library.canonicalReference().ifPresent(image -> addUnique(merged, seen, image));
        for (CharacterReferenceImage image : library.images()) {
            if (image.identityAnchor()) {
                addUnique(merged, seen, image);
            }
        }
        for (CharacterReferenceImage image : selected) {
            addUnique(merged, seen, image);
        }

        return merged.stream().limit(maxPerRequest).toList();
    }

    private List<CharacterReferenceImage> pickIdentityAnchors(CharacterReferenceLibrary library) {
        List<CharacterReferenceImage> anchors = new ArrayList<>();
        library.canonicalReference().ifPresent(anchors::add);
        library.images().stream()
                .filter(CharacterReferenceImage::identityAnchor)
                .filter(image -> anchors.stream().noneMatch(existing -> existing.id().equals(image.id())))
                .forEach(anchors::add);
        library.images().stream()
                .filter(image -> image.imageType() == ReferenceImageType.FACE
                        || image.imageType() == ReferenceImageType.CANONICAL
                        || image.imageType() == ReferenceImageType.FULL_BODY)
                .filter(image -> anchors.stream().noneMatch(existing -> existing.id().equals(image.id())))
                .forEach(anchors::add);
        return anchors.stream().limit(maxPerRequest).toList();
    }

    private static void addUnique(
            List<CharacterReferenceImage> merged,
            Set<String> seen,
            CharacterReferenceImage image) {
        if (seen.add(image.id())) {
            merged.add(image);
        }
    }

    private List<ScoredReference> scoreLibrary(CharacterReferenceLibrary library, SceneReferenceHints hints) {
        List<ScoredReference> scored = new ArrayList<>();
        for (CharacterReferenceImage image : library.images()) {
            int score = image.priority();
            if (image.identityAnchor()) {
                score += SCORE_CANONICAL_IDENTITY;
            }
            if (image.imageType() == ReferenceImageType.CANONICAL || image.imageType() == ReferenceImageType.FACE) {
                score += SCORE_FACE_IDENTITY;
            }
            if (hints.cameraAngles().contains(image.cameraAngle())) {
                score += SCORE_CAMERA_MATCH;
            }
            if (hints.framing().contains(image.framing())) {
                score += SCORE_FRAMING_MATCH;
            }
            if (hints.poses().contains(image.pose())) {
                score += SCORE_POSE_MATCH;
            }
            if (hints.expressions().contains(image.expression())) {
                score += SCORE_EXPRESSION_MATCH;
            }
            if (hints.actions().contains(image.action())) {
                score += SCORE_ACTION_MATCH;
            }
            if (hints.canonicalClothing() && image.clothing() == ReferenceClothing.CANONICAL) {
                score += SCORE_CLOTHING_MATCH;
            }
            scored.add(new ScoredReference(image, score));
        }
        scored.sort(Comparator.comparingInt(ScoredReference::score).reversed()
                .thenComparing(scoredRef -> scoredRef.image().id()));
        return scored;
    }

    /**
     * Always include one identity anchor, then greedily add complementary references up to maxPerRequest.
     */
    private List<CharacterReferenceImage> pickDiverseReferences(
            List<ScoredReference> scored,
            CharacterReferenceLibrary library) {

        List<CharacterReferenceImage> selected = new ArrayList<>();
        Set<String> selectedIds = new HashSet<>();
        Set<String> diversityBuckets = new HashSet<>();

        library.canonicalReference().ifPresent(canonical -> {
            selected.add(canonical);
            selectedIds.add(canonical.id());
            diversityBuckets.add(diversityBucket(canonical));
        });

        if (selected.isEmpty()) {
            scored.stream()
                    .filter(scoredRef -> scoredRef.image().identityAnchor())
                    .findFirst()
                    .ifPresent(scoredRef -> {
                        selected.add(scoredRef.image());
                        selectedIds.add(scoredRef.image().id());
                        diversityBuckets.add(diversityBucket(scoredRef.image()));
                    });
        }

        for (ScoredReference candidate : scored) {
            if (selected.size() >= maxPerRequest) {
                break;
            }
            if (selectedIds.contains(candidate.image().id())) {
                continue;
            }
            String bucket = diversityBucket(candidate.image());
            int bonus = diversityBuckets.contains(bucket) ? 0 : SCORE_DIVERSITY_BONUS;
            if (bonus == 0 && selected.size() >= minPerRequest) {
                continue;
            }
            selected.add(candidate.image());
            selectedIds.add(candidate.image().id());
            diversityBuckets.add(bucket);
        }

        while (selected.size() < minPerRequest) {
            boolean added = false;
            for (ScoredReference candidate : scored) {
                if (selectedIds.add(candidate.image().id())) {
                    selected.add(candidate.image());
                    added = true;
                    break;
                }
            }
            if (!added) {
                break;
            }
        }

        return List.copyOf(selected);
    }

    private ReferenceSelectionResult legacyFallback(RoleplayCharacter character, VisualImageStorageService storage) {
        List<String> paths = visualIdentityService.resolveReferenceImagePaths(character, storage);
        return new ReferenceSelectionResult(List.of(), paths, "legacy");
    }

    private static String diversityBucket(CharacterReferenceImage image) {
        return image.imageType() + "|" + image.framing() + "|" + image.cameraAngle();
    }

    private record ScoredReference(CharacterReferenceImage image, int score) {
    }
}
