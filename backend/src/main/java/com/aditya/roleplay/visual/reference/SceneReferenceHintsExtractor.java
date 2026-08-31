package com.aditya.roleplay.visual.reference;

import com.aditya.roleplay.model.visual.VisualCharacterScenePresence;
import com.aditya.roleplay.model.visual.VisualSceneState;
import com.aditya.roleplay.model.visual.reference.ReferenceAction;
import com.aditya.roleplay.model.visual.reference.ReferenceCameraAngle;
import com.aditya.roleplay.model.visual.reference.ReferenceExpression;
import com.aditya.roleplay.model.visual.reference.ReferenceFraming;
import com.aditya.roleplay.model.visual.reference.ReferencePose;
import com.aditya.roleplay.model.visual.reference.SceneReferenceHints;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

@ApplicationScoped
public class SceneReferenceHintsExtractor {

    public SceneReferenceHints extract(VisualSceneState sceneState, VisualCharacterScenePresence presence) {
        String camera = combine(sceneState != null ? sceneState.camera() : null);
        String pose = combine(presence != null ? presence.pose() : null);
        String action = combine(presence != null ? presence.action() : null);
        String expression = combine(presence != null ? presence.expression() : null);
        String sceneClothing = presence != null ? presence.sceneClothing() : null;
        String combined = combine(camera, pose, action, expression);

        Set<ReferenceCameraAngle> cameraAngles = EnumSet.noneOf(ReferenceCameraAngle.class);
        Set<ReferenceFraming> framing = EnumSet.noneOf(ReferenceFraming.class);
        Set<ReferencePose> poses = EnumSet.noneOf(ReferencePose.class);
        Set<ReferenceExpression> expressions = EnumSet.noneOf(ReferenceExpression.class);
        Set<ReferenceAction> actions = EnumSet.noneOf(ReferenceAction.class);

        addCameraHints(cameraAngles, framing, combined, camera);
        addPoseHints(poses, actions, combined, pose, action);
        addExpressionHints(expressions, combined, expression);

        boolean canonicalClothing = sceneClothing == null || sceneClothing.isBlank();
        return new SceneReferenceHints(cameraAngles, framing, poses, expressions, actions, canonicalClothing);
    }

    private static void addCameraHints(
            Set<ReferenceCameraAngle> cameraAngles,
            Set<ReferenceFraming> framing,
            String combined,
            String camera) {
        String text = combined + " " + defaultString(camera);
        if (containsAny(text, "close up", "close-up", "closeup", "portrait", "face shot")) {
            cameraAngles.add(ReferenceCameraAngle.CLOSE_UP);
            framing.add(ReferenceFraming.CLOSE_UP);
        }
        if (containsAny(text, "medium shot", "medium", "waist up", "waist-up", "mid shot")) {
            cameraAngles.add(ReferenceCameraAngle.MEDIUM);
            framing.add(ReferenceFraming.MEDIUM);
        }
        if (containsAny(text, "full body", "full-body", "full shot", "wide shot")) {
            cameraAngles.add(ReferenceCameraAngle.FULL_BODY);
            framing.add(ReferenceFraming.FULL_BODY);
        }
        if (containsAny(text, "low angle", "from below", "looking up at")) {
            cameraAngles.add(ReferenceCameraAngle.LOW_ANGLE);
        }
        if (containsAny(text, "high angle", "from above", "bird")) {
            cameraAngles.add(ReferenceCameraAngle.HIGH_ANGLE);
        }
        if (containsAny(text, "side view", "profile", "side angle", "side shot")) {
            cameraAngles.add(ReferenceCameraAngle.SIDE);
        }
        if (containsAny(text, "back view", "from behind", "rear view", "walking away")) {
            cameraAngles.add(ReferenceCameraAngle.BACK);
        }
        if (containsAny(text, "three quarter", "three-quarter", "3/4", "angled")) {
            cameraAngles.add(ReferenceCameraAngle.THREE_QUARTER);
        }
        if (containsAny(text, "front view", "facing camera", "eye level", "front shot")) {
            cameraAngles.add(ReferenceCameraAngle.FRONT);
        }
    }

    private static void addPoseHints(
            Set<ReferencePose> poses,
            Set<ReferenceAction> actions,
            String combined,
            String pose,
            String action) {
        String text = combined + " " + defaultString(pose) + " " + defaultString(action);
        if (containsAny(text, "sit", "seated", "sitting", "throne", "steps")) {
            poses.add(ReferencePose.SITTING);
            actions.add(ReferenceAction.RESTING);
        }
        if (containsAny(text, "kneel", "kneeling", "on one knee")) {
            poses.add(ReferencePose.KNEELING);
            actions.add(ReferenceAction.RESTING);
        }
        if (containsAny(text, "lean", "leaning", "against pillar", "against wall")) {
            poses.add(ReferencePose.LEANING);
            actions.add(ReferenceAction.RESTING);
        }
        if (containsAny(text, "walk", "walking", "stride", "moving")) {
            poses.add(ReferencePose.WALKING);
            actions.add(ReferenceAction.WALKING);
        }
        if (containsAny(text, "reach", "reaching", "extended hand", "outstretched")) {
            poses.add(ReferencePose.REACHING);
            actions.add(ReferenceAction.REACHING);
        }
        if (containsAny(text, "combat", "fight", "battle", "stance", "crouch", "defensive")) {
            poses.add(ReferencePose.COMBAT);
            actions.add(ReferenceAction.COMBAT);
        }
        if (containsAny(text, "cast", "casting", "spell", "magic")) {
            poses.add(ReferencePose.CASTING);
            actions.add(ReferenceAction.CASTING);
        }
        if (containsAny(text, "rest", "resting", "peaceful", "calm", "window")) {
            poses.add(ReferencePose.RESTING);
            actions.add(ReferenceAction.RESTING);
        }
        if (containsAny(text, "stand", "standing", "upright")) {
            poses.add(ReferencePose.STANDING);
            actions.add(ReferenceAction.STANDING);
        }
        if (containsAny(text, "listen", "listening", "talk", "conversation")) {
            actions.add(ReferenceAction.LISTENING);
        }
    }

    private static void addExpressionHints(
            Set<ReferenceExpression> expressions,
            String combined,
            String expression) {
        String text = combined + " " + defaultString(expression);
        if (containsAny(text, "happy", "smile", "smiling", "joy")) {
            expressions.add(ReferenceExpression.HAPPY);
        }
        if (containsAny(text, "angry", "furious", "rage")) {
            expressions.add(ReferenceExpression.ANGRY);
        }
        if (containsAny(text, "sad", "melancholic", "sorrow", "grief")) {
            expressions.add(ReferenceExpression.SAD);
        }
        if (containsAny(text, "serious", "stern", "focused")) {
            expressions.add(ReferenceExpression.SERIOUS);
        }
        if (containsAny(text, "determined", "resolve", "intense")) {
            expressions.add(ReferenceExpression.DETERMINED);
        }
        if (containsAny(text, "surprised", "shock", "startled")) {
            expressions.add(ReferenceExpression.SURPRISED);
        }
        if (containsAny(text, "thoughtful", "contemplative", "distant", "looking down")) {
            expressions.add(ReferenceExpression.THOUGHTFUL);
        }
        if (containsAny(text, "serene", "peaceful", "calm", "tranquil")) {
            expressions.add(ReferenceExpression.SERENE);
        }
        if (containsAny(text, "playful", "innocent")) {
            expressions.add(ReferenceExpression.PLAYFUL);
        }
        if (containsAny(text, "neutral")) {
            expressions.add(ReferenceExpression.NEUTRAL);
        }
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String combine(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append(' ');
                }
                builder.append(part);
            }
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private static String defaultString(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
