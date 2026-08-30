export interface RoleplayCharacter {
  id: string;
  worldId: string;
  name: string;
  imageUrl?: string;
  personality: string[];
  background: string;
  speakingStyle: string;
  values: string[];
}

export interface World {
  id: string;
  name: string;
  description: string;
  rules: string[];
}

export interface CharacterDetailResponse {
  character: RoleplayCharacter;
  world: World;
}
