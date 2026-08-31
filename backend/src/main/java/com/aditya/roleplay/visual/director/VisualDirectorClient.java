package com.aditya.roleplay.visual.director;

import com.aditya.roleplay.model.visual.director.VisualDirectorRequest;
import com.aditya.roleplay.model.visual.director.VisualScenePlan;

import java.util.Optional;

public interface VisualDirectorClient {

    Optional<VisualScenePlan> plan(VisualDirectorRequest request);
}
