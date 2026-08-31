from __future__ import annotations

from typing import TypedDict

from app.models.visual_plan import (
  CharacterInteraction,
  CharacterSelection,
  InteractionPlan,
  SceneAnalysis,
  SceneComposition,
  VisualContextSelection,
  VisualDirectorRequest,
  VisualIdentityContext,
  VisualPlanCharacter,
  VisualScenePlan,
)


class VisualGraphState(TypedDict, total=False):
  request: VisualDirectorRequest
  scene_analysis: SceneAnalysis
  should_generate: bool
  character_selection: CharacterSelection
  selected_identities: list[VisualIdentityContext]
  visual_context: VisualContextSelection
  interaction_plan: InteractionPlan
  scene_composition: SceneComposition
  plan_characters: list[VisualPlanCharacter]
  visual_prompt: str
  negative_prompt: str
  reasoning_summary: str
  consistency_corrections: list[str]
