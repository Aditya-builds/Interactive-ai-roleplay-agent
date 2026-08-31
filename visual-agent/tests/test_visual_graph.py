from __future__ import annotations

import pytest

from app.graph.visual_graph import run_visual_graph
from app.models.visual_plan import (
  CandidateCharacter,
  CharacterStateContext,
  MessageContext,
  RelationshipContext,
  SceneContext,
  VisualDirectorRequest,
  VisualIdentityContext,
)


def _base_request(**overrides):
  payload = {
    "conversationId": "conv-1",
    "focalCharacterId": "aurora",
    "playerPersonaName": "Aurora Player",
    "explicitGeneration": False,
    "scene": SceneContext(
      location="guild_hall",
      locationDescription="Stone guild hall with torchlight",
      time="evening",
      currentSituation="Aurora and Laxus talk tensely.",
      charactersPresent=["aurora", "laxus", "user"],
    ),
    "characterState": CharacterStateContext(characterId="aurora", emotion="guarded", status="standing"),
    "recentMessages": [],
    "relationships": [
      RelationshipContext(targetId="user", trust=42, respect=67, affection=12, familiarity=54, suspicion=8),
      RelationshipContext(sourceId="aurora", targetId="laxus", trust=35, respect=72, affection=5, familiarity=48, suspicion=15),
    ],
    "candidateCharacters": [
      CandidateCharacter(characterId="aurora", name="Aurora", inScene=True),
      CandidateCharacter(characterId="laxus", name="Laxus", inScene=True),
      CandidateCharacter(characterId="mira", name="Mira", inScene=False),
    ],
    "visualIdentities": [
      VisualIdentityContext(
        characterId="aurora",
        canonicalReferenceImage="/api/visuals/references/aurora",
        visualDescription="Ethereal dark fantasy Aurora",
        faceDescription="delicate face",
        hairDescription="jet-black long hair",
        eyeDescription="dark almond eyes",
        skinDescription="pale porcelain skin",
        bodyDescription="slender athletic build",
        clothingDescription="ethereal pale cream outfit",
        artStyle="dark fantasy cinematic",
        negativePrompt="blonde hair",
      ),
      VisualIdentityContext(
        characterId="laxus",
        canonicalReferenceImage="/api/visuals/references/laxus",
        visualDescription="Bold lightning mage",
        hairDescription="blond hair",
        artStyle="dark fantasy cinematic",
      ),
    ],
  }
  payload.update(overrides)
  return VisualDirectorRequest(**payload)


def test_trivial_conversation_does_not_recommend_generation():
  plan = run_visual_graph(
    _base_request(
      recentMessages=[
        MessageContext(id="1", role="user", content="Okay."),
        MessageContext(id="2", role="assistant", content="Right."),
      ]
    )
  )
  assert plan.shouldGenerate is False


def test_major_action_recommends_generation():
  plan = run_visual_graph(
    _base_request(
      recentMessages=[
        MessageContext(
          id="1",
          role="assistant",
          content="Aurora slowly draws her sword as the guild hall erupts into chaos.",
        )
      ]
    )
  )
  assert plan.shouldGenerate is True
  assert plan.momentType == "COMBAT"


def test_explicit_generation_overrides_trivial_turn():
  plan = run_visual_graph(
    _base_request(
      explicitGeneration=True,
      recentMessages=[MessageContext(id="1", role="user", content="Okay.")],
    )
  )
  assert plan.shouldGenerate is True
  assert plan.prompt


def test_two_character_conversation_includes_both_primary_characters():
  plan = run_visual_graph(
    _base_request(
      explicitGeneration=True,
      recentMessages=[
        MessageContext(id="1", role="user", content="Aurora, what does Laxus want?"),
        MessageContext(id="2", role="assistant", content="Aurora quietly looks at Laxus, her expression softening."),
      ],
    )
  )
  ids = {character.characterId for character in plan.characters}
  assert "aurora" in ids
  assert "laxus" in ids
  assert plan.momentType == "EMOTIONAL_INTERACTION"


def test_irrelevant_character_excluded():
  plan = run_visual_graph(_base_request(explicitGeneration=True))
  ids = {character.characterId for character in plan.characters}
  assert "mira" not in ids


def test_relationship_context_affects_body_language():
  plan = run_visual_graph(
    _base_request(
      explicitGeneration=True,
      recentMessages=[
        MessageContext(id="1", role="assistant", content="Aurora and Laxus face each other with guarded tension."),
      ],
      relationships=[
        RelationshipContext(targetId="laxus", trust=20, respect=40, affection=5, familiarity=30, suspicion=55),
      ],
    )
  )
  assert plan.interaction.emotionalTension in {"moderate", "high", "low"}
  assert "CHARACTER INTERACTIONS" in plan.prompt


def test_canonical_identity_protection_in_prompt():
  plan = run_visual_graph(_base_request(explicitGeneration=True))
  assert "/api/visuals/references/aurora" in plan.prompt
  assert "Preserve face" in plan.prompt
  assert "blonde hair" in plan.negativePrompt


def test_aurora_reference_image_selected():
  plan = run_visual_graph(_base_request(explicitGeneration=True))
  aurora = next(character for character in plan.characters if character.characterId == "aurora")
  assert aurora.referenceImage == "/api/visuals/references/aurora"
