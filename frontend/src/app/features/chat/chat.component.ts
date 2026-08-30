import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { ConversationApiService } from '../../core/services/conversation-api.service';
import { CharacterApiService } from '../../core/services/character-api.service';
import { formatLocationSlug, Message, Scene } from '../../core/models/conversation.model';
import { MessageListComponent } from './components/message-list/message-list.component';
import { MessageInputComponent } from './components/message-input/message-input.component';
import { LoadingSpinnerComponent } from '../../shared/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, RouterLink, MessageListComponent, MessageInputComponent, LoadingSpinnerComponent],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss'
})
export class ChatComponent implements OnInit, OnDestroy {
  @ViewChild('scrollContainer') scrollContainer?: ElementRef<HTMLDivElement>;

  conversationId = '';
  characterId = '';
  characterName = '';
  characterImageUrl = '';
  worldName = '';
  sceneLocation = '';
  scene: Scene | null = null;
  messages: Message[] = [];
  loading = true;
  sending = false;
  restarting = false;
  error = '';
  private routeSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private conversationApi: ConversationApiService,
    private characterApi: CharacterApiService
  ) {}

  ngOnInit(): void {
    this.routeSub = this.route.paramMap.subscribe(params => {
      this.conversationId = params.get('conversationId') ?? '';
      this.messages = [];
      this.loadConversation();
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  loadConversation(): void {
    this.loading = true;
    this.error = '';

    this.conversationApi.getConversation(this.conversationId).subscribe({
      next: (conversation) => {
        this.messages = conversation.messages;
        this.characterId = conversation.characterId;
        this.updateScene(conversation.scene);
        this.loading = false;
        this.loadCharacterInfo(conversation.characterId);
        this.scrollToBottom();
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = err.error?.error ?? 'Failed to load conversation.';
      }
    });
  }

  loadCharacterInfo(characterId: string): void {
    this.characterApi.getCharacter(characterId).subscribe({
      next: (detail) => {
        this.characterName = detail.character.name;
        this.worldName = detail.world.name;
        this.characterImageUrl = detail.character.imageUrl ?? '';
      }
    });
  }

  startFresh(): void {
    if (!this.characterId || this.restarting) {
      return;
    }

    this.restarting = true;
    this.error = '';

    this.conversationApi.deleteConversation(this.conversationId).subscribe({
      next: () => this.createNewConversation(),
      error: () => this.createNewConversation()
    });
  }

  private createNewConversation(): void {
    this.conversationApi.createConversation(this.characterId).subscribe({
      next: (conversation) => {
        this.restarting = false;
        this.router.navigate(['/chat', conversation.id]);
      },
      error: (err: HttpErrorResponse) => {
        this.restarting = false;
        this.error = err.error?.error ?? 'Failed to start a fresh conversation.';
      }
    });
  }

  onSend(content: string): void {
    this.sending = true;
    this.error = '';

    const optimisticUserMessage: Message = {
      id: `temp-${Date.now()}`,
      role: 'user',
      content,
      timestamp: new Date().toISOString()
    };
    this.messages = [...this.messages, optimisticUserMessage];
    this.scrollToBottom();

    this.conversationApi.sendMessage(this.conversationId, content).subscribe({
      next: (response) => {
        this.messages = [
          ...this.messages.filter(m => m.id !== optimisticUserMessage.id),
          { ...optimisticUserMessage, id: `user-${Date.now()}` },
          response.message
        ];
        this.updateScene(response.scene);
        this.sending = false;
        this.scrollToBottom();
      },
      error: (err: HttpErrorResponse) => {
        this.messages = this.messages.filter(m => m.id !== optimisticUserMessage.id);
        this.sending = false;
        this.error = err.error?.error ?? 'Failed to send message. Please try again.';
      }
    });
  }

  private updateScene(scene: Scene): void {
    this.scene = scene;
    this.sceneLocation = formatLocationSlug(scene.location);
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
