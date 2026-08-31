from __future__ import annotations

import re

from app.graph.state import VisualGraphState
from app.models.visual_plan import SceneAnalysis

_ACTION_WORDS = (
  "draw", "sword", "attack", "fight", "run", "grab", "hold", "give", "offer",
  "embrace", "hug", "kiss", "strike", "cast", "spell", "explod", "chaos", "draws",
)
_EMOTION_WORDS = (
  "soften", "tear", "smile", "glare", "hesitat", "tender", "angry", "furious",
  "sad", "grief", "love", "fear", "wary", "guard",
)
_TRIVIAL_PATTERNS = (
  r"^\s*ok(?:ay)?\s*\.?\s*$",
  r"^\s*yes\s*\.?\s*$",
  r"^\s*no\s*\.?\s*$",
  r"^\s*hi\s*\.?\s*$",
  r"^\s*hello\s*\.?\s*$",
  r"^\s*thanks?\s*\.?\s*$",
)


def _latest_user_message(state: VisualGraphState) -> str:
  messages = state["request"].recentMessages
  for message in reversed(messages):
    if message.role == "user":
      return message.content.strip()
  return ""


def _latest_assistant_message(state: VisualGraphState) -> str:
  messages = state["request"].recentMessages
  for message in reversed(messages):
    if message.role == "assistant":
      return message.content.strip()
  return ""


def _is_trivial(text: str) -> bool:
  lowered = text.lower().strip()
  if not lowered:
    return True
  return any(re.match(pattern, lowered) for pattern in _TRIVIAL_PATTERNS)


def analyze_scene(state: VisualGraphState) -> VisualGraphState:
  request = state["request"]
  user_text = _latest_user_message(state)
  assistant_text = _latest_assistant_message(state)
  combined = f"{user_text} {assistant_text}".lower()

  major_action = any(word in combined for word in _ACTION_WORDS)
  emotional = any(word in combined for word in _EMOTION_WORDS)
  location_changed = bool(request.scene and request.scene.location)
  characters_changed = bool(request.scene and len(request.scene.charactersPresent) > 1)

  moment_type = "CONVERSATION"
  if major_action and ("fight" in combined or "sword" in combined or "attack" in combined):
    moment_type = "COMBAT"
  elif emotional and characters_changed:
    moment_type = "EMOTIONAL_INTERACTION"
  elif major_action:
    moment_type = "ACTION"
  elif emotional:
    moment_type = "EMOTIONAL_INTERACTION"

  trivial = _is_trivial(user_text) and not major_action and not emotional
  if request.explicitGeneration:
    importance = "HIGH"
    recommended = True
  elif trivial:
    importance = "LOW"
    recommended = False
  elif major_action or emotional:
    importance = "HIGH"
    recommended = True
  elif len(combined) > 40:
    importance = "MEDIUM"
    recommended = True
  else:
    importance = "LOW"
    recommended = False

  analysis = SceneAnalysis(
    visualImportance=importance,
    momentType=moment_type,
    locationChanged=location_changed,
    charactersChanged=characters_changed,
    majorAction=major_action,
    emotionalMoment=emotional,
    recommendedGeneration=recommended,
  )
  return {
    **state,
    "scene_analysis": analysis,
    "should_generate": recommended or request.explicitGeneration,
  }


def detect_visual_moment(state: VisualGraphState) -> VisualGraphState:
  analysis = state.get("scene_analysis")
  if not analysis:
    return analyze_scene(state)
  return state
