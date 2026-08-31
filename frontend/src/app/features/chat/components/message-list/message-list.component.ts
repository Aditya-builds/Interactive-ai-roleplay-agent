import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Message } from '../../../../core/models/conversation.model';
import { MessageBubbleComponent } from '../message-bubble/message-bubble.component';
import { SceneImageCardComponent } from '../../../../shared/scene-image-card/scene-image-card.component';
import { SceneImageApiService } from '../../../../core/services/scene-image-api.service';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-message-list',
  standalone: true,
  imports: [CommonModule, MessageBubbleComponent, SceneImageCardComponent],
  templateUrl: './message-list.component.html',
  styleUrl: './message-list.component.scss'
})
export class MessageListComponent {
  @Input({ required: true }) messages: Message[] = [];
  @Input({ required: true }) characterName!: string;
  @Input() characterImageUrl = '';
  @Input() playerName = 'You';
  @Input() playerImageUrl = '';
  @Input() typingMessageId: string | null = null;
  @Input() regenerating = false;

  @Output() typingUpdate = new EventEmitter<void>();
  @Output() regenerate = new EventEmitter<void>();

  readonly showSceneImageDebug = environment.showSceneImageDebug;

  constructor(private sceneImageApi: SceneImageApiService) {}

  lastAssistantMessageId(): string | null {
    for (let index = this.messages.length - 1; index >= 0; index--) {
      const message = this.messages[index];
      if (message.role === 'assistant' && !message.sceneImageId) {
        return message.id;
      }
    }
    return null;
  }

  sceneImageUrl(sceneImageId: string): string {
    return this.sceneImageApi.sceneImageContentUrl(sceneImageId);
  }
}