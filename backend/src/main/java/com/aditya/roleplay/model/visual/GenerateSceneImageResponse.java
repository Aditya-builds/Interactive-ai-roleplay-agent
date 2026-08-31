package com.aditya.roleplay.model.visual;

import com.aditya.roleplay.model.Message;

public record GenerateSceneImageResponse(
        GeneratedSceneImage sceneImage,
        Message sceneImageMessage) {
}
