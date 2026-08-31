import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription, switchMap } from 'rxjs';
import { ConversationApiService } from '../../core/services/conversation-api.service';
import { CharacterApiService } from '../../core/services/character-api.service';
import { PersonaApiService } from '../../core/services/roleplay-setup-api.service';
import {
  CharacterRuntimeState,
  Conversation,
  Message,
  Relationship,
  Scene
} from '../../core/models/conversation.model';
import { MessageListComponent } from './components/message-list/message-list.component';
import { MessageInputComponent } from './components/message-input/message-input.component';
import { StatePanelComponent } from './components/state-panel/state-panel.component';
import { LoadingSpinnerComponent } from '../../shared/loading-spinner/loading-spinner.component';
import { ActorPortraitComponent } from '../../shared/actor-portrait/actor-portrait.component';
import { ApiKeySettingsComponent } from '../../shared/api-key-settings/api-key-settings.component';
import { SceneImageApiService } from '../../core/services/scene-image-api.service';
import { apiUrl, resolveActorImageUrl } from '../../core/config/api-url';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MessageListComponent,
    MessageInputComponent,
    StatePanelComponent,
    LoadingSpinnerComponent,
    ActorPortraitComponent,
    ApiKeySettingsComponent
  ],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss'
})
export class ChatComponent implements OnInit, OnDestroy {
  @ViewChild('scrollContainer') scrollContainer?: ElementRef<HTMLDivElement>;

  conversationId = '';
  characterId = '';
  characterName = '';
  characterImageUrl = '';
  playerName = 'You';
  playerImageUrl = '';
  scene: Scene | null = null;
  characterState: CharacterRuntimeState | null = null;
  playerPersonaId?: string;
  relationships: Relationship[] = [];
  messages: Message[] = [];
  loading = true;
  sending = false;
  generatingScene = false;
  restarting = false;
  sendError = false;
  sceneError = '';
  pendingMessage = '';
  private routeSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private conversationApi: ConversationApiService,
    private characterApi: CharacterApiService,
    private personaApi: PersonaApiService,
    private sceneImageApi: SceneImageApiService
  ) {}

  ngOnInit(): void {
    this.routeSub = this.route.paramMap.subscribe(params => {
      this.conversationId = params.get('conversationId') ?? '';
      this.resetView();
      this.loadConversation();
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  get consideringLabel(): string {
    const name = this.characterName || 'The character';
    return `${name} is considering what to say...`;
  }

  get inputDisabled(): boolean {
    return this.loading || this.sending || this.restarting || this.generatingScene;
  }

  loadConversation(): void {
    this.loading = true;
    this.sendError = false;

    this.conversationApi.getConversation(this.conversationId).subscribe({
      next: (conversation) => {
        this.applyConversation(conversation);
        this.loading = false;
        this.loadCharacterInfo(conversation.characterId);
        this.loadPlayerPersonaInfo(conversation.playerPersonaId);
        this.scrollToBottom();
      },
      error: () => {
        this.loading = false;
        this.sendError = true;
      }
    });
  }

  loadCharacterInfo(characterId: string): void {
    this.characterApi.getCharacter(characterId).subscribe({
      next: (detail) => {
        this.characterName = detail.character.name;
        this.characterImageUrl = resolveActorImageUrl(detail.character.imageUrl ?? '');
      }
    });
  }

  loadPlayerPersonaInfo(playerPersonaId?: string): void {
    if (!playerPersonaId) {
      this.playerName = 'You';
      this.playerImageUrl = '';
      return;
    }

    this.personaApi.getPersona(playerPersonaId).subscribe({
      next: (persona) => {
        this.playerName = persona.name;
        this.playerImageUrl = resolveActorImageUrl(persona.imageUrl ?? '');
      },
      error: () => {
        this.playerName = 'You';
        this.playerImageUrl = '';
      }
    });
  }

  startFresh(): void {
    if (!this.characterId || this.restarting) {
      return;
    }

    this.restarting = true;
    this.sendError = false;

    this.conversationApi.deleteConversation(this.conversationId).subscribe({
      next: () => this.createNewConversation(),
      error: () => this.createNewConversation()
    });
  }

  onSend(content: string): void {
    if (this.sending) {
      return;
    }

    this.sending = true;
    this.sendError = false;
    this.pendingMessage = content;

    const optimisticUserMessage: Message = {
      id: `temp-${Date.now()}`,
      role: 'user',
      content,
      timestamp: new Date().toISOString()
    };
    this.messages = [...this.messages, optimisticUserMessage];
    this.scrollToBottom();

    this.conversationApi.sendMessage(this.conversationId, content).pipe(
      switchMap(() => this.conversationApi.getConversation(this.conversationId))
    ).subscribe({
      next: (conversation) => {
        this.applyConversation(conversation);
        this.pendingMessage = '';
        this.sending = false;
        this.scrollToBottom();
      },
      error: () => {
        this.messages = this.messages.filter(m => m.id !== optimisticUserMessage.id);
        this.sending = false;
        this.sendError = true;
      }
    });
  }

  retrySend(): void {
    if (!this.pendingMessage || this.sending) {
      if (!this.pendingMessage && this.loading) {
        this.loadConversation();
      }
      return;
    }
    this.onSend(this.pendingMessage);
  }

  generateSceneImage(): void {
    if (!this.conversationId || this.generatingScene) {
      return;
    }

    this.generatingScene = true;
    this.sceneError = '';

    this.sceneImageApi.generateSceneImage(this.conversationId).pipe(
      switchMap(() => this.conversationApi.getConversation(this.conversationId))
    ).subscribe({
      next: (conversation) => {
        this.applyConversation(conversation);
        this.generatingScene = false;
        this.scrollToBottom();
      },
      error: (err: HttpErrorResponse) => {
        this.generatingScene = false;
        this.sceneError = err.error?.error ?? 'Scene image generation failed. Is the backend running?';
      }
    });
  }

  private createNewConversation(): void {
    this.conversationApi.createLegacyConversation(this.characterId).subscribe({
      next: (conversation) => {
        this.restarting = false;
        this.router.navigate(['/chat', conversation.id]);
      },
      error: () => {
        this.restarting = false;
        this.sendError = true;
      }
    });
  }

  private applyConversation(conversation: Conversation): void {
    this.messages = [...conversation.messages];
    this.characterId = conversation.characterId;
    this.scene = conversation.scene ? { ...conversation.scene, charactersPresent: [...conversation.scene.charactersPresent] } : null;
    this.characterState = conversation.characterState
      ? {
          ...conversation.characterState,
          health: { ...conversation.characterState.health }
        }
      : null;
    this.relationships = conversation.relationships.map(r => ({ ...r }));
    this.playerPersonaId = conversation.playerPersonaId;
  }

  private resetView(): void {
    this.messages = [];
    this.scene = null;
    this.characterState = null;
    this.playerPersonaId = undefined;
    this.playerName = 'You';
    this.playerImageUrl = '';
    this.relationships = [];
    this.sendError = false;
    this.sceneError = '';
    this.pendingMessage = '';
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const el = this.scrollContainer?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    });
  }
}
