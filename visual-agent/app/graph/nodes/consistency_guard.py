from __future__ import annotations

from app.graph.state import VisualGraphState
from app.models.visual_plan import VisualPlanCharacter


_IDENTITY_FIELDS = (
  "faceDescription",
  "hairDescription",
  "eyeDescription",
  "skinDescription",
  "bodyDescription",
)


def consistency_guard(state: VisualGraphState) -> VisualGraphState:
  request = state["request"]
  interaction = state["interaction_plan"]
  identity_map = {identity.characterId: identity for identity in state.get("selected_identities", [])}
  corrections: list[str] = []

  plan_characters: list[VisualPlanCharacter] = []
  for interaction_character in interaction.characters:
    if interaction_character.characterId in {"user", "player"}:
      continue
    identity = identity_map.get(interaction_character.characterId)
    reference = identity.canonicalReferenceImage if identity else None
    scene_clothing = identity.clothingDescription if identity else None

    if identity:
      for field in _IDENTITY_FIELDS:
        if not getattr(identity, field, None):
          corrections.append(f"Missing canonical {field} for {interaction_character.characterId}; using reference image as source of truth.")

    plan_characters.append(
      VisualPlanCharacter(
        characterId=interaction_character.characterId,
        name=interaction_character.name,
        referenceImage=reference,
        pose=interaction_character.pose,
        expression=interaction_character.expression,
        action=interaction_character.action,
        position=interaction_character.position,
        sceneClothing=scene_clothing,
      )
    )

  if not plan_characters:
    focal = request.focalCharacterId
    identity = identity_map.get(focal)
    plan_characters.append(
      VisualPlanCharacter(
        characterId=focal,
        name=_character_name(state, focal),
        referenceImage=identity.canonicalReferenceImage if identity else None,
        pose="natural stance",
        expression=request.characterState.emotion if request.characterState and request.characterState.emotion else "neutral",
        action="present in scene",
        position="center foreground",
        sceneClothing=identity.clothingDescription if identity else None,
      )
    )

  return {**state, "plan_characters": plan_characters, "consistency_corrections": corrections}


def _character_name(state: VisualGraphState, character_id: str) -> str:
  for candidate in state["request"].candidateCharacters:
    if candidate.characterId == character_id:
      return candidate.name
  return character_id
