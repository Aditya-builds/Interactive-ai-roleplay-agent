import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ConversationSummary } from '../../../../core/models/conversation.model';

@Component({
  selector: 'app-chat-history-panel',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './chat-history-panel.component.html',
  styleUrl: './chat-history-panel.component.scss'
})
export class ChatHistoryPanelComponent {
  @Input() conversations: ConversationSummary[] = [];
  @Input() loading = false;
  @Input() deletingId: string | null = null;
  @Output() deleteConversation = new EventEmitter<string>();

  groupedConversations(): { characterId: string; characterName: string; items: ConversationSummary[] }[] {
    const groups = new Map<string, { characterId: string; characterName: string; items: ConversationSummary[] }>();
    for (const conversation of this.conversations) {
      const existing = groups.get(conversation.characterId);
      if (existing) {
        existing.items.push(conversation);
      } else {
        groups.set(conversation.characterId, {
          characterId: conversation.characterId,
          characterName: conversation.characterName,
          items: [conversation]
        });
      }
    }
    return Array.from(groups.values());
  }

  formatDate(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '';
    }
    return date.toLocaleString(undefined, {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit'
    });
  }

  onDelete(event: MouseEvent, conversationId: string): void {
    event.preventDefault();
    event.stopPropagation();
    this.deleteConversation.emit(conversationId);
  }
}
