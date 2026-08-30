import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="spinner" [class.inline]="inline">
      <div class="dot"></div>
      <div class="dot"></div>
      <div class="dot"></div>
      @if (label) {
        <span class="label">{{ label }}</span>
      }
    </div>
  `,
  styles: [`
    .spinner {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 2rem 1rem;
      justify-content: center;
    }

    .spinner.inline {
      padding: 0.5rem 0;
    }

    .dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: var(--accent, #8b9cb3);
      animation: bounce 1.4s infinite ease-in-out both;
    }

    .dot:nth-child(1) { animation-delay: -0.32s; }
    .dot:nth-child(2) { animation-delay: -0.16s; }

    @keyframes bounce {
      0%, 80%, 100% { opacity: 0.3; transform: scale(0.85); }
      40% { opacity: 1; transform: scale(1); }
    }

    .label {
      color: var(--text-muted, #6b7280);
      font-size: 0.875rem;
    }
  `]
})
export class LoadingSpinnerComponent {
  @Input() label = '';
  @Input() inline = false;
}
