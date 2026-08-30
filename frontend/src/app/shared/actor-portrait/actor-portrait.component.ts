import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-actor-portrait',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="portrait"
      [class.portrait-xs]="size === 'xs'"
      [class.portrait-sm]="size === 'sm'"
      [class.portrait-md]="size === 'md'"
    >
      @if (hasImage) {
        <img
          [src]="src"
          [alt]="alt || name"
          (error)="onError()"
          loading="lazy"
        />
      } @else {
        <div class="fallback" [attr.aria-label]="alt || name">
          <span class="initial">{{ initial }}</span>
        </div>
      }
    </div>
  `,
  styles: [`
    .portrait {
      position: relative;
      overflow: hidden;
      border-radius: 8px;
      background: var(--surface-overlay, #181d26);
      border: 1px solid var(--border-subtle, #252b36);
      flex-shrink: 0;
    }

    .portrait img {
      display: block;
      width: 100%;
      height: 100%;
      object-fit: cover;
      object-position: top center;
    }

    .fallback {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 100%;
      background: #151922;
    }

    .initial {
      font-size: 1.75rem;
      font-weight: 600;
      letter-spacing: 0.04em;
      color: var(--text-muted, #6b7280);
      text-transform: uppercase;
      user-select: none;
    }

    .portrait-xs {
      width: 100%;
      aspect-ratio: 3 / 4;
      max-height: 100px;
    }

    .portrait-xs .initial {
      font-size: 1.1rem;
    }

    .portrait-sm {
      width: 48px;
      height: 48px;
    }

    .portrait-sm .initial {
      font-size: 1rem;
    }

    .portrait-md {
      width: 100%;
      aspect-ratio: 3 / 4;
      max-height: 140px;
    }

    :host(.portrait-lg) .portrait {
      width: 100%;
      aspect-ratio: 3 / 4;
      max-height: 160px;
    }

    :host(.portrait-lg) .initial {
      font-size: 2rem;
    }
  `]
})
export class ActorPortraitComponent implements OnChanges {
  @Input() src?: string | null;
  @Input() alt = '';
  @Input() name = '';
  @Input() size: 'xs' | 'sm' | 'md' | 'lg' = 'md';

  failed = false;

  ngOnChanges(): void {
    this.failed = false;
  }

  get hasImage(): boolean {
    return !!this.src?.trim() && !this.failed;
  }

  get initial(): string {
    const trimmed = this.name?.trim();
    return trimmed ? trimmed.charAt(0).toUpperCase() : '?';
  }

  onError(): void {
    this.failed = true;
  }
}
