import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Message } from '../../../../core/models/conversation.model';
import { ActorPortraitComponent } from '../../../../shared/actor-portrait/actor-portrait.component';

@Component({
  selector: 'app-message-bubble',
  standalone: true,
  imports: [CommonModule, ActorPortraitComponent],
  templateUrl: './message-bubble.component.html',
  styleUrl: './message-bubble.component.scss'
})
export class MessageBubbleComponent {
  @Input({ required: true }) message!: Message;
  @Input({ required: true }) characterName!: string;
  @Input() characterImageUrl = '';
}
