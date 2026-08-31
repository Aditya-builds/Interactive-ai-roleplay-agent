import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges
} from '@angular/core';
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
export class MessageBubbleComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) message!: Message;
  @Input({ required: true }) characterName!: string;
  @Input() characterImageUrl = '';
  @Input() playerName = 'You';
  @Input() playerImageUrl = '';
  @Input() animateTyping = false;
  @Input() showRegenerate = false;
  @Input() regenerating = false;

  @Output() typingUpdate = new EventEmitter<void>();
  @Output() regenerate = new EventEmitter<void>();

  displayedContent = '';
  isTyping = false;

  private typingTimer?: ReturnType<typeof setTimeout>;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['message'] || changes['animateTyping']) {
      this.resetTyping();
      if (this.message.role === 'assistant' && this.animateTyping) {
        this.startTyping(this.message.content);
      } else {
        this.displayedContent = this.message.content;
      }
    }
  }

  ngOnDestroy(): void {
    this.resetTyping();
  }

  private startTyping(text: string): void {
    if (!text) {
      this.displayedContent = '';
      return;
    }

    const { chunk, delay } = this.computeTypingPlan(text.length);
    let index = 0;
    this.isTyping = true;
    this.displayedContent = '';

    const step = (): void => {
      index = Math.min(index + chunk, text.length);
      this.displayedContent = text.slice(0, index);
      this.typingUpdate.emit();

      if (index < text.length) {
        this.typingTimer = setTimeout(step, delay);
      } else {
        this.isTyping = false;
      }
    };

    this.typingTimer = setTimeout(step, delay);
  }

  private computeTypingPlan(length: number): { chunk: number; delay: number } {
    if (length > 1200) {
      return { chunk: 4, delay: 10 };
    }
    if (length > 600) {
      return { chunk: 3, delay: 14 };
    }
    if (length > 250) {
      return { chunk: 2, delay: 18 };
    }
    return { chunk: 1, delay: 22 };
  }

  private resetTyping(): void {
    if (this.typingTimer) {
      clearTimeout(this.typingTimer);
      this.typingTimer = undefined;
    }
    this.isTyping = false;
  }
}
