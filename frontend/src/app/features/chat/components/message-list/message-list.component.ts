import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Message } from '../../../../core/models/conversation.model';
import { MessageBubbleComponent } from '../message-bubble/message-bubble.component';

@Component({
  selector: 'app-message-list',
  standalone: true,
  imports: [CommonModule, MessageBubbleComponent],
  templateUrl: './message-list.component.html',
  styleUrl: './message-list.component.scss'
})
export class MessageListComponent {
  @Input({ required: true }) messages: Message[] = [];
  @Input({ required: true }) characterName!: string;
  @Input() characterImageUrl = '';
  @Input() playerName = 'You';
  @Input() playerImageUrl = '';
}
