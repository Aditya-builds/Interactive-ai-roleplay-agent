from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field


class MessageContext(BaseModel):
  id: str
  role: str
  content: str


class SceneContext(BaseModel):
  location: str | None = None
  locationDescription: str | None = None
  time: str | None = None
  currentSituation: str | None = None
  currentConflict: str | None = None
  charactersPresent: list[str] = Field(default_factory=list)


class CharacterStateContext(BaseModel):
  characterId: str
  emotion: str | None = None
  status: str | None = None
  location: str | None = None


class RelationshipContext(BaseModel):
  sourceId: str | None = None
  targetId: str
  trust: int = 0
  respect: int = 0
  affection: int = 0
  familiarity: int = 0
  suspicion: int = 0


class EventContext(BaseModel):
  description: str
  importance: float | None = None


class CandidateCharacter(BaseModel):
  characterId: str
  name: str
  inScene: bool = False


class VisualIdentityContext(BaseModel):
  characterId: str
  canonicalReferenceImage: str | None = None
  visualDescription: str | None = None
  faceDescription: str | None = None
  hairDescription: str | None = None
  eyeDescription: str | None = None
  skinDescription: str | None = None
  bodyDescription: str | None = None
  clothingDescription: str | None = None
  accessories: list[str] = Field(default_factory=list)
  artStyle: str | None = None
  negativePrompt: str | None = None


class VisualDirectorRequest(BaseModel):
  conversationId: str
  focalCharacterId: str
  playerPersonaName: str | None = None
  explicitGeneration: bool = False
  scene: SceneContext | None = None
  characterState: CharacterStateContext | None = None
  recentMessages: list[MessageContext] = Field(default_factory=list)
  recentEvents: list[EventContext] = Field(default_factory=list)
  relationships: list[RelationshipContext] = Field(default_factory=list)
  candidateCharacters: list[CandidateCharacter] = Field(default_factory=list)
  visualIdentities: list[VisualIdentityContext] = Field(default_factory=list)


class SceneAnalysis(BaseModel):
  visualImportance: Literal["LOW", "MEDIUM", "HIGH"] = "LOW"
  momentType: str = "CONVERSATION"
  locationChanged: bool = False
  charactersChanged: bool = False
  majorAction: bool = False
  emotionalMoment: bool = False
  recommendedGeneration: bool = False


class CharacterSelection(BaseModel):
  primaryCharacters: list[str] = Field(default_factory=list)
  secondaryCharacters: list[str] = Field(default_factory=list)
  backgroundCharacters: list[str] = Field(default_factory=list)
  excludedCharacters: list[str] = Field(default_factory=list)


class CharacterInteraction(BaseModel):
  characterId: str
  name: str
  position: str
  pose: str
  expression: str
  gaze: str
  action: str


class InteractionPlan(BaseModel):
  characters: list[CharacterInteraction] = Field(default_factory=list)
  focus: str = ""
  distance: str = ""
  bodyLanguage: str = ""
  emotionalTension: str = ""


class SceneComposition(BaseModel):
  location: str = ""
  locationDescription: str = ""
  time: str = ""
  environment: str = ""
  lighting: str = ""
  atmosphere: str = ""
  camera: str = ""
  framing: str = ""
  composition: str = ""
  background: str = ""


class VisualContextSelection(BaseModel):
  recentMoment: str = ""
  relationshipNotes: str = ""
  relevantEvents: str = ""
  excludedNotes: str = ""


class VisualPlanCharacter(BaseModel):
  characterId: str
  name: str
  referenceImage: str | None = None
  pose: str = ""
  expression: str = ""
  action: str = ""
  position: str = ""
  sceneClothing: str | None = None


class VisualScenePlan(BaseModel):
  shouldGenerate: bool = False
  momentType: str = "CONVERSATION"
  reasoningSummary: str = ""
  characters: list[VisualPlanCharacter] = Field(default_factory=list)
  scene: SceneComposition = Field(default_factory=SceneComposition)
  interaction: InteractionPlan = Field(default_factory=InteractionPlan)
  prompt: str = ""
  negativePrompt: str = ""
  graphExecutionMs: int = 0


class VisualDirectorResponse(BaseModel):
  plan: VisualScenePlan
