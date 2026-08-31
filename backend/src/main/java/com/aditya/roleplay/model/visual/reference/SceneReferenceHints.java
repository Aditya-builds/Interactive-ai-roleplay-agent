package com.aditya.roleplay.model.visual.reference;

import java.util.EnumSet;
import java.util.Set;

/**
 * Normalized scene hints extracted from free-text planner output for reference scoring.
 */
public record SceneReferenceHints(
        Set<ReferenceCameraAngle> cameraAngles,
        Set<ReferenceFraming> framing,
        Set<ReferencePose> poses,
        Set<ReferenceExpression> expressions,
        Set<ReferenceAction> actions,
        boolean canonicalClothing) {

    public SceneReferenceHints {
        cameraAngles = cameraAngles != null ? EnumSet.copyOf(cameraAngles) : EnumSet.noneOf(ReferenceCameraAngle.class);
        framing = framing != null ? EnumSet.copyOf(framing) : EnumSet.noneOf(ReferenceFraming.class);
        poses = poses != null ? EnumSet.copyOf(poses) : EnumSet.noneOf(ReferencePose.class);
        expressions = expressions != null ? EnumSet.copyOf(expressions) : EnumSet.noneOf(ReferenceExpression.class);
        actions = actions != null ? EnumSet.copyOf(actions) : EnumSet.noneOf(ReferenceAction.class);
    }
}
