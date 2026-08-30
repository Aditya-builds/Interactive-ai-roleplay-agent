import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-actor-portrait',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="portrait" [class.portrait-sm]="size === 'sm'" [class.portrait-md]="size === 'md'">
      @if (src && !failed) {
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
      background: linear-gradient(160deg, #1a2030 0%, #12161d 100%);
    }

    .initial {
      font-size: 1.75rem;
      font-weight: 600;
      letter-spacing: 0.04em;
      color: var(--text-muted, #6b7280);
      text-transform: uppercase;
      user-select: none;
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
      max-height: 180px;
    }

    :host(.portrait-lg) .initial {
      font-size: 2.25rem;
    }
  `]
})
export class ActorPortraitComponent {
  @Input() src?: string | null;
  @Input() alt = '';
  @Input() name = '';
  @Input() size: 'sm' | 'md' | 'lg' = 'md';

  failed = false;

  get initial(): string {
    const trimmed = this.name?.trim();
    return trimmed ? trimmed.charAt(0).toUpperCase() : '?';
  }

  onError(): void {
    this.failed = true;
  }
}
