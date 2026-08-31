from __future__ import annotations

from app.graph.state import VisualGraphState
from app.models.visual_plan import VisualIdentityContext


def retrieve_visual_identities(state: VisualGraphState) -> VisualGraphState:
  request = state["request"]
  selection = state["character_selection"]
  selected_ids = set(
    selection.primaryCharacters
    + selection.secondaryCharacters
    + selection.backgroundCharacters
  )
  identity_map = {identity.characterId: identity for identity in request.visualIdentities}
  selected = [identity_map[cid] for cid in selected_ids if cid in identity_map and cid not in {"user", "player"}]
  return {**state, "selected_identities": selected}
