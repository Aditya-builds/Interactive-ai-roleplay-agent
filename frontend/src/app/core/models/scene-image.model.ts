export interface GeneratedSceneImage {
  id: string;
  conversationId: string;
  characterIds: string[];
  sourceMessageId?: string;
  prompt: string;
  negativePrompt?: string;
  provider: string;
  model: string;
  imageUrl: string;
  createdAt: string;
  selectedReferenceIds?: string[];
  selectedReferenceCount?: number;
  referenceSelectionSummary?: string;
}

export interface GenerateSceneImageResponse {
  sceneImage: GeneratedSceneImage;
  sceneImageMessage: {
    id: string;
    role: string;
    content: string;
    timestamp: string;
    sceneImageId: string;
  };
}
