export interface ActorProfile {
  description?: string;
  personality: string[];
  background?: string;
  values?: string[];
  speakingStyle?: string;
}

export interface Appearance {
  height?: number;
  hair?: string;
  eyes?: string;
  build?: string;
  description?: string;
}

export interface PlayerPersona {
  id: string;
  name: string;
  worldId?: string;
  imageUrl?: string;
  profile: ActorProfile;
  appearance?: Appearance;
  abilities?: string[];
  goals?: string[];
}

export interface Story {
  id: string;
  title: string;
  worldId: string;
  premise: string;
  openingNarrative?: string;
  startingCharacters: string[];
  startingLocation?: string;
  storyRules: string[];
}

export interface CreateConversationRequest {
  characterId?: string;
  playerPersonaId?: string;
  storyId?: string;
  focalCharacterId?: string;
  activeCharacterIds?: string[];
}
