export type MessageRole = 'user' | 'assistant';

export interface Message {
  id: string;
  role: MessageRole;
  content: string;
  timestamp: string;
}

export interface Scene {
  location: string;
  time: string;
  charactersPresent: string[];
  currentSituation: string;
  currentConflict: string | null;
}

export interface CharacterRuntimeState {
  characterId: string;
  health: { current: number; max: number };
  status: string | null;
  emotion: string | null;
}

export interface Relationship {
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
  characterState?: CharacterRuntimeState;
  scene: Scene;
  relationships: Relationship[];
  memories: StoryMemoryEntry[];
  messages: Message[];
}

export interface SendMessageResponse {
  message: Message;
  conversationId: string;
  scene: Scene;
  relationships: Relationship[];
}

export interface ApiError {
  error: string;
  code: string;
}

export function formatLocationSlug(slug: string): string {
  if (!slug || slug === 'unknown') {
    return 'Unknown';
  }
  return slug
    .split('_')
    .map(part => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}
