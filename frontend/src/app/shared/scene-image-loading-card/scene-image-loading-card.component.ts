import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActorPortraitComponent } from '../actor-portrait/actor-portrait.component';

@Component({
  selector: 'app-scene-image-loading-card',
  standalone: true,
  imports: [CommonModule, ActorPortraitComponent],
  templateUrl: './scene-image-loading-card.component.html',
  styleUrl: './scene-image-loading-card.component.scss'
})
export class SceneImageLoadingCardComponent implements OnInit, OnDestroy {
  @Input({ required: true }) characterName!: string;
  @Input() characterImageUrl = '';

  statusIndex = 0;
  private statusTimer?: ReturnType<typeof setInterval>;

  readonly statusMessages = [
    'Composing the scene...',
    'Selecting reference poses...',
    'Matching character identity...',
    'Painting lighting and atmosphere...',
    'Rendering final image...'
  ];

  ngOnInit(): void {
    this.statusTimer = setInterval(() => {
      this.statusIndex = (this.statusIndex + 1) % this.statusMessages.length;
    }, 2400);
  }

  ngOnDestroy(): void {
    if (this.statusTimer) {
      clearInterval(this.statusTimer);
    }
  }

  get statusLabel(): string {
    return this.statusMessages[this.statusIndex];
  }
}
