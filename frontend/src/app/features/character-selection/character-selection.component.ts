import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CharacterApiService } from '../../core/services/character-api.service';
import { ConversationApiService } from '../../core/services/conversation-api.service';
import { PersonaApiService, StoryApiService } from '../../core/services/roleplay-setup-api.service';
import { RoleplayCharacter } from '../../core/models/character.model';
import { ConversationSummary } from '../../core/models/conversation.model';
import { PlayerPersona, Story } from '../../core/models/roleplay-setup.model';
import { LoadingSpinnerComponent } from '../../shared/loading-spinner/loading-spinner.component';
import { ActorPortraitComponent } from '../../shared/actor-portrait/actor-portrait.component';
import { ApiKeySettingsComponent } from '../../shared/api-key-settings/api-key-settings.component';
import { ChatHistoryPanelComponent } from './components/chat-history-panel/chat-history-panel.component';
import { PersonaPickCardComponent } from './components/persona-pick-card/persona-pick-card.component';
import { resolveActorImageUrl } from '../../core/config/api-url';

@Component({
  selector: 'app-character-selection',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    LoadingSpinnerComponent,
    ActorPortraitComponent,
    ApiKeySettingsComponent,
    ChatHistoryPanelComponent,
    PersonaPickCardComponent
  ],
  templateUrl: './character-selection.component.html',
  styleUrl: './character-selection.component.scss'
})
export class CharacterSelectionComponent implements OnInit {
  stories: Story[] = [];
  personas: PlayerPersona[] = [];
  characters: RoleplayCharacter[] = [];

  selectedStory: Story | null = null;
  selectedPersona: PlayerPersona | null = null;
  selectedCharacter: RoleplayCharacter | null = null;

  loading = true;
  starting = false;
  loadError = false;
  startError = false;
  quickStartOpen = false;
  chatHistory: ConversationSummary[] = [];
  historyLoading = false;
  deletingConversationId: string | null = null;

  constructor(
    private storyApi: StoryApiService,
    private personaApi: PersonaApiService,
    private characterApi: CharacterApiService,
    private conversationApi: ConversationApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadSetupData();
    this.loadChatHistory();
  }

  loadChatHistory(): void {
    this.historyLoading = true;
    this.conversationApi.listConversations().subscribe({
      next: conversations => {
        this.chatHistory = conversations;
        this.historyLoading = false;
      },
      error: () => {
        this.historyLoading = false;
      }
    });
  }

  deleteHistoryConversation(conversationId: string): void {
    if (!confirm('Delete this chat permanently?')) {
      return;
    }
    this.deletingConversationId = conversationId;
    this.conversationApi.deleteConversation(conversationId).subscribe({
      next: () => {
        this.chatHistory = this.chatHistory.filter(item => item.id !== conversationId);
        this.deletingConversationId = null;
      },
      error: () => {
        this.deletingConversationId = null;
      }
    });
  }

  loadSetupData(): void {
    this.loading = true;
    this.loadError = false;
    this.startError = false;

    forkJoin({
      stories: this.storyApi.listStories(),
      personas: this.personaApi.listPersonas(),
      characters: this.characterApi.listCharacters()
    }).subscribe({
      next: ({ stories, personas, characters }) => {
        this.stories = stories;
        this.personas = personas;
        this.characters = characters;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.loadError = true;
      }
    });
  }

  selectStory(story: Story): void {
    this.selectedStory = story;
    this.selectedPersona = null;
    this.selectedCharacter = null;
    this.startError = false;
  }

  selectPersona(persona: PlayerPersona): void {
    this.selectedPersona = persona;
    this.selectedCharacter = null;
    this.startError = false;
  }

  selectCharacter(character: RoleplayCharacter): void {
    this.selectedCharacter = character;
    this.startError = false;
  }

  personasForStory(): PlayerPersona[] {
    if (!this.selectedStory) {
      return [];
    }
    return this.personas.filter(
      p => !p.worldId || p.worldId === this.selectedStory!.worldId
    );
  }

  charactersForStory(): RoleplayCharacter[] {
    if (!this.selectedStory) {
      return [];
    }
    const starting = new Set(this.selectedStory.startingCharacters);
    return this.characters.filter(
      c => c.worldId === this.selectedStory!.worldId && starting.has(c.id)
    );
  }

  storyCharacters(story: Story): RoleplayCharacter[] {
    const ids = new Set(story.startingCharacters ?? []);
    return this.characters.filter(c => ids.has(c.id));
  }

  legacyCharacters(): RoleplayCharacter[] {
    const storyWorldIds = new Set(this.stories.map(s => s.worldId));
    return this.characters.filter(c => !storyWorldIds.has(c.worldId));
  }

  canStartStorySession(): boolean {
    return !!(this.selectedStory && this.selectedPersona && this.selectedCharacter);
  }

  currentStep(): number {
    if (!this.selectedStory) return 1;
    if (!this.selectedPersona) return 2;
    if (!this.selectedCharacter) return 3;
    return 4;
  }

  startStorySession(): void {
    if (!this.canStartStorySession() || this.starting) {
      return;
    }

    this.starting = true;
    this.startError = false;

    this.conversationApi.createConversation({
      playerPersonaId: this.selectedPersona!.id,
      storyId: this.selectedStory!.id,
      focalCharacterId: this.selectedCharacter!.id,
      activeCharacterIds: [this.selectedCharacter!.id]
    }).subscribe({
      next: (conversation) => {
        this.router.navigate(['/chat', conversation.id]);
      },
      error: () => {
        this.starting = false;
        this.startError = true;
      }
    });
  }

  startLegacyConversation(character: RoleplayCharacter): void {
    if (this.starting) {
      return;
    }

    this.starting = true;
    this.startError = false;

    this.conversationApi.createLegacyConversation(character.id).subscribe({
      next: (conversation) => {
        this.router.navigate(['/chat', conversation.id]);
      },
      error: () => {
        this.starting = false;
        this.startError = true;
      }
    });
  }

  personalitySummary(items: string[] | undefined): string {
    if (!items?.length) {
      return '';
    }
    return items.slice(0, 3).join(' · ');
  }

  /** Single blurb — no duplicate hook + premise. */
  storyBlurb(story: Story): string {
    const premise = story.premise?.trim() ?? '';
    if (premise.length <= 180) {
      return premise;
    }
    return premise.slice(0, 177).trimEnd() + '…';
  }

  storyTagList(story: Story): string[] {
    const tags = [this.formatLabel(story.worldId)];
    if (story.startingLocation) {
      tags.push(this.formatLabel(story.startingLocation));
    }
    return tags;
  }

  personaRoleLine(persona: PlayerPersona): string {
    const description = persona.profile.description?.trim() ?? '';
    if (description.length <= 72) {
      return description;
    }
    return description.slice(0, 69).trimEnd() + '…';
  }

  formatLabel(value: string): string {
    return value
      .split('_')
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  actorImageUrl(url?: string | null): string {
    return resolveActorImageUrl(url);
  }
}

