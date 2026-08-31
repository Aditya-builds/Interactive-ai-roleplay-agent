from __future__ import annotations

from app.graph.state import VisualGraphState
from app.models.visual_plan import CharacterInteraction, InteractionPlan


def _character_name(state: VisualGraphState, character_id: str) -> str:
  for candidate in state["request"].candidateCharacters:
    if candidate.characterId == character_id:
      return candidate.name
  if character_id == "user":
    return state["request"].playerPersonaName or "the player"
  return character_id


def _emotion_from_state(state: VisualGraphState) -> str:
  cs = state["request"].characterState
  if cs and cs.emotion:
    return cs.emotion
  return "attentive"


def direct_character_interaction(state: VisualGraphState) -> VisualGraphState:
  request = state["request"]
  selection = state["character_selection"]
  context = state["visual_context"]
  combined = f"{context.recentMoment} {context.relationshipNotes}".lower()

  interactions: list[CharacterInteraction] = []
  primary = selection.primaryCharacters[:2]

  positions = ["left foreground", "right foreground", "center background"]
  for index, character_id in enumerate(primary):
    if character_id in {"user", "player"}:
      interactions.append(
        CharacterInteraction(
          characterId="user",
          name=_character_name(state, "user"),
          position=positions[index] if index < len(positions) else "midground",
          pose="engaged stance",
          expression="attentive",
          gaze=_character_name(state, primary[1 - index]) if len(primary) > 1 else "forward",
          action="participating in the current moment",
        )
      )
      continue

    pose = "natural stance"
    action = "present in the scene"
    if any(word in combined for word in ("hold", "hand", "give", "offer", "embrace")):
      pose = "close interaction, hands visible"
      action = "physically interacting with the other character"
    elif any(word in combined for word in ("draw", "sword", "attack", "fight")):
      pose = "dynamic action pose"
      action = "mid-action in a tense moment"

    interactions.append(
      CharacterInteraction(
        characterId=character_id,
        name=_character_name(state, character_id),
        position=positions[index] if index < len(positions) else "midground",
        pose=pose,
        expression=_emotion_from_state(state) if character_id == request.focalCharacterId else "responsive",
        gaze=_character_name(state, primary[1 - index]) if len(primary) > 1 else "forward",
        action=action,
      )
    )

  tension = "low"
  if "suspicion" in combined or any(rel.suspicion > 30 for rel in request.relationships):
    tension = "moderate"
  if any(word in combined for word in ("fight", "chaos", "attack", "angry")):
    tension = "high"

  focus = " and ".join(_character_name(state, cid) for cid in primary if cid not in {"user", "player"})
  plan = InteractionPlan(
    characters=interactions,
    focus=focus or _character_name(state, request.focalCharacterId),
    distance="about two meters" if len(primary) > 1 else "single-character focus",
    bodyLanguage="facing each other with visible emotional engagement" if len(primary) > 1 else "centered portrait energy",
    emotionalTension=tension,
  )
  return {**state, "interaction_plan": plan}
