from __future__ import annotations

from app.graph.state import VisualGraphState
from app.models.visual_plan import CharacterSelection


def select_characters(state: VisualGraphState) -> VisualGraphState:
  request = state["request"]
  scene_present = set(request.scene.charactersPresent if request.scene else [])
  focal = request.focalCharacterId

  primary: list[str] = [focal]
  secondary: list[str] = []
  background: list[str] = []
  excluded: list[str] = []

  for candidate in request.candidateCharacters:
    cid = candidate.characterId
    if cid == focal:
      continue
    if cid in {"user", "player"}:
      continue
    if cid in scene_present:
      if len(primary) < 2:
        primary.append(cid)
      elif len(secondary) < 2:
        secondary.append(cid)
      else:
        background.append(cid)
    else:
      excluded.append(cid)

  if "user" in scene_present or "player" in scene_present:
    if "user" not in primary and "player" not in primary:
      secondary.insert(0, "user")

  selection = CharacterSelection(
    primaryCharacters=primary,
    secondaryCharacters=secondary,
    backgroundCharacters=background,
    excludedCharacters=excluded,
  )
  return {**state, "character_selection": selection}
