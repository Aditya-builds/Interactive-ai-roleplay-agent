from __future__ import annotations

from app.graph.state import VisualGraphState
from app.models.visual_plan import VisualContextSelection


def _truncate(text: str, limit: int = 280) -> str:
  cleaned = " ".join(text.split())
  if len(cleaned) <= limit:
    return cleaned
  return cleaned[: limit - 3] + "..."


def select_relevant_context(state: VisualGraphState) -> VisualGraphState:
  request = state["request"]
  messages = request.recentMessages[-4:]
  player = request.playerPersonaName or "the player"
  focal_name = next(
    (c.name for c in request.candidateCharacters if c.characterId == request.focalCharacterId),
    request.focalCharacterId,
  )

  recent_lines: list[str] = []
  for message in messages:
    speaker = player if message.role == "user" else focal_name
    recent_lines.append(f"{speaker}: {_truncate(message.content)}")

  relationship_notes: list[str] = []
  for rel in request.relationships:
    if rel.targetId == "user":
      relationship_notes.append(
        f"trust {rel.trust}, respect {rel.respect}, affection {rel.affection}, suspicion {rel.suspicion}"
      )

  relevant_events = [
    _truncate(event.description, 120)
    for event in request.recentEvents[:3]
    if event.description
  ]

  excluded = state["character_selection"].excludedCharacters
  excluded_notes = f"Excluded from image: {', '.join(excluded)}" if excluded else ""

  visual_context = VisualContextSelection(
    recentMoment="\n".join(recent_lines),
    relationshipNotes="; ".join(relationship_notes),
    relevantEvents="; ".join(relevant_events),
    excludedNotes=excluded_notes,
  )
  return {**state, "visual_context": visual_context}
