package com.aditya.roleplay.model.visual.reference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CharacterReferenceImage(
        String id,
        String filename,
        ReferenceImageType imageType,
        ReferencePose pose,
        ReferenceCameraAngle cameraAngle,
        ReferenceExpression expression,
        ReferenceClothing clothing,
        ReferenceAction action,
        ReferenceFraming framing,
        String notes,
        int priority,
        boolean identityAnchor) {

    public CharacterReferenceImage {
        if (id == null) {
            id = "";
        }
        if (filename == null) {
            filename = "";
        }
        if (imageType == null) {
            imageType = ReferenceImageType.UNKNOWN;
        }
        if (pose == null) {
            pose = ReferencePose.UNKNOWN;
        }
        if (cameraAngle == null) {
            cameraAngle = ReferenceCameraAngle.UNKNOWN;
        }
        if (expression == null) {
            expression = ReferenceExpression.UNKNOWN;
        }
        if (clothing == null) {
            clothing = ReferenceClothing.UNKNOWN;
        }
        if (action == null) {
            action = ReferenceAction.UNKNOWN;
        }
        if (framing == null) {
            framing = ReferenceFraming.UNKNOWN;
        }
    }
}
