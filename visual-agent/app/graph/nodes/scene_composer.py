from __future__ import annotations

from app.graph.state import VisualGraphState
from app.models.visual_plan import SceneComposition


def compose_scene(state: VisualGraphState) -> VisualGraphState:
  request = state["request"]
  scene = request.scene
  interaction = state["interaction_plan"]

  location = scene.location if scene and scene.location else "unknown"
  location_description = scene.locationDescription if scene and scene.locationDescription else location
  time = scene.time if scene and scene.time else "unspecified"
  situation = scene.currentSituation if scene and scene.currentSituation else ""
  conflict = scene.currentConflict if scene and scene.currentConflict else ""

  camera = "medium-wide cinematic two-character shot" if len(interaction.characters) > 1 else "medium cinematic shot"
  lighting = "balanced cinematic lighting"
  lowered_time = time.lower()
  if "evening" in lowered_time or "night" in lowered_time:
    lighting = "warm evening interior light with soft shadows"
  elif "morning" in lowered_time or "dawn" in lowered_time:
    lighting = "cool morning light with gentle highlights"

  atmosphere = "immersive fantasy atmosphere"
  if conflict:
    atmosphere = f"tense atmosphere, {conflict}"

  foreground = ", ".join(
    f"{character.name} ({character.position})" for character in interaction.characters[:2]
  )
  composition = SceneComposition(
    location=location,
    locationDescription=location_description,
    time=time,
    environment=location_description,
    lighting=lighting,
    atmosphere=atmosphere,
    camera=camera,
    framing="cinematic roleplay scene",
    composition=f"Foreground: {foreground}. Preserve authoritative location/time from scene state.",
    background=f"{location_description}; subdued background activity only",
  )
  if situation:
    composition = composition.model_copy(update={"environment": f"{location_description}. {situation}"})
  return {**state, "scene_composition": composition}
