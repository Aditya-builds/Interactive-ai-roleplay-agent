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

export interface Relationship {
  characterId: string;
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
}

export interface Conversation {
  id: string;
  characterId: string;
  worldId: string;
  createdAt: string;
  updatedAt: string;
  scene: Scene;
  relationship: Relationship;
  memories: StoryMemoryEntry[];
  messages: Message[];
}

export interface SendMessageResponse {
  message: Message;
  conversationId: string;
  scene: Scene;
  relationship: Relationship;
}

export interface ApiError {
  error: string;
  code: string;
}
