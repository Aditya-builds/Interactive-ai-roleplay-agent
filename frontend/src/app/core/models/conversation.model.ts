export type MessageRole = 'user' | 'assistant';

export interface Message {
  id: string;
  role: MessageRole;
  content: string;
  timestamp: string;
  sceneImageId?: string;
}

export interface Scene {
  location: string;
  userLocation?: string;
  time: string;
  charactersPresent: string[];
  currentSituation: string | null;
  currentConflict: string | null;
}

export interface CharacterRuntimeState {
  characterId: string;
  health: { current: number; max: number };
  location?: string;
  status: string | null;
  emotion: string | null;
}

export interface Relationship {
  sourceId?: string;
  targetId: string;
  trust: number;
  respect: number;
  affection: number;
  familiarity: number;
  suspicion: number;
}

export interface StoryMemoryEntry {
  id: string;
  content: string;
  createdAt: string;
  source: string;
  importance?: number;
  tags?: string[];
  relatedCharacterIds?: string[];
}

export interface Conversation {
  id: string;
  characterId: string;
  worldId: string;
  createdAt: string;
  updatedAt: string;
  playerPersonaId?: string;
  storyId?: string;
  activeCharacterIds?: string[];
  characterState?: CharacterRuntimeState;
  playerPersonaState?: CharacterRuntimeState;
  scene: Scene;
  relationships: Relationship[];
  memories: StoryMemoryEntry[];
  messages: Message[];
}

export interface SendMessageResponse {
  message: Message;
  conversationId: string;
  scene: Scene;
  characterState?: CharacterRuntimeState;
  relationships: Relationship[];
}

export interface ApiError {
  error: string;
  code: string;
}

export function formatLocationSlug(slug: string | null | undefined): string {
  if (!slug || slug === 'unknown') {
    return 'Unknown';
  }
  return slug
    .split('_')
    .map(part => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export function formatTimeSlug(slug: string | null | undefined): string {
  if (!slug || slug === 'unknown') {
    return 'Unknown';
  }
  return formatLocationSlug(slug);
}

export function formatPresentName(id: string, characterName: string): string {
  if (id === 'user') {
    return 'You';
  }
  if (id === characterName.toLowerCase() || id === characterName) {
    return characterName;
  }
  return formatLocationSlug(id);
}

export function formatStatusLabel(status: string | null | undefined, health?: { current: number; max: number }): string {
  if (status) {
    return status.charAt(0).toUpperCase() + status.slice(1);
  }
  if (health && health.current >= health.max) {
    return 'Healthy';
  }
  if (health && health.current < health.max) {
    return 'Injured';
  }
  return 'Unknown';
}

export function formatEmotionLabel(emotion: string | null | undefined): string {
  if (!emotion) {
    return 'Neutral';
  }
  return emotion.charAt(0).toUpperCase() + emotion.slice(1);
}

export function userRelationship(
  relationships: Relationship[],
  playerPersonaId?: string
): Relationship | undefined {
  if (playerPersonaId) {
    const personaRel = relationships.find(r => r.targetId === playerPersonaId);
    if (personaRel) {
      return personaRel;
    }
  }
  return relationships.find(r => r.targetId === 'user');
}
