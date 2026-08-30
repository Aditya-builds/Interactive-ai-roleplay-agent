import { Component, ElementRef, HostListener, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiKeyService } from '../../core/services/api-key.service';

@Component({
  selector: 'app-api-key-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="api-key-settings">
      <button
        type="button"
        class="toggle-btn"
        [class.has-key]="apiKeyService.hasKey()"
        (click)="togglePanel($event)"
        aria-haspopup="dialog"
        [attr.aria-expanded]="open"
      >
        <span class="status-dot" aria-hidden="true"></span>
        API Key
      </button>

      @if (open) {
        <div class="panel" role="dialog" aria-label="OpenAI API key settings">
          <p class="panel-title">OpenAI API Key</p>
          <p class="panel-hint">
            Stored locally in your browser only. Used for AI responses — never shared with other users.
          </p>

          <label class="field-label" for="api-key-input">Your key</label>
          <input
            id="api-key-input"
            class="field-input"
            type="password"
            autocomplete="off"
            spellcheck="false"
            placeholder="sk-..."
            [(ngModel)]="draftKey"
          />

          @if (savedMessage) {
            <p class="saved-msg">{{ savedMessage }}</p>
          }

          <div class="panel-actions">
            <button type="button" class="btn-save" (click)="save()">Save</button>
            @if (apiKeyService.hasKey()) {
              <button type="button" class="btn-clear" (click)="clear()">Remove</button>
            }
          </div>

          @if (apiKeyService.hasKey()) {
            <p class="key-preview">Saved: {{ apiKeyService.maskedPreview() }}</p>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .api-key-settings {
      position: relative;
    }

    .toggle-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      padding: 0.35rem 0.6rem;
      border: 1px solid var(--border-subtle, #252b36);
      border-radius: 6px;
      background: rgba(18, 22, 29, 0.6);
      color: var(--text-secondary, #a8b0bd);
      font-size: 0.76rem;
      font-weight: 500;
      cursor: pointer;
    }

    .toggle-btn:hover {
      border-color: var(--border-strong, #343b48);
      color: var(--text-primary, #e8eaed);
    }

    .toggle-btn.has-key {
      border-color: rgba(139, 156, 179, 0.45);
    }

    .status-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: var(--text-muted, #6b7280);
    }

    .toggle-btn.has-key .status-dot {
      background: #6ee7a0;
    }

    .panel {
      position: absolute;
      top: calc(100% + 0.45rem);
      right: 0;
      z-index: 40;
      width: min(18rem, calc(100vw - 2rem));
      padding: 0.85rem;
      border: 1px solid var(--border-subtle, #252b36);
      border-radius: 10px;
      background: var(--surface-raised, #12161d);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
    }

    .panel-title {
      margin: 0 0 0.25rem;
      font-size: 0.78rem;
      font-weight: 600;
      color: var(--text-primary, #e8eaed);
    }

    .panel-hint {
      margin: 0 0 0.65rem;
      font-size: 0.72rem;
      line-height: 1.4;
      color: var(--text-muted, #6b7280);
    }

    .field-label {
      display: block;
      margin-bottom: 0.25rem;
      font-size: 0.68rem;
      font-weight: 600;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      color: var(--text-muted, #6b7280);
    }

    .field-input {
      width: 100%;
      padding: 0.5rem 0.6rem;
      border: 1px solid var(--border-subtle, #252b36);
      border-radius: 6px;
      background: var(--surface-base, #0c0f14);
      color: var(--text-primary, #e8eaed);
      font-size: 0.82rem;
      font-family: inherit;
    }

    .field-input:focus {
      outline: none;
      border-color: var(--accent, #8b9cb3);
    }

    .saved-msg {
      margin: 0.45rem 0 0;
      font-size: 0.72rem;
      color: #6ee7a0;
    }

    .panel-actions {
      display: flex;
      gap: 0.45rem;
      margin-top: 0.65rem;
    }

    .btn-save,
    .btn-clear {
      padding: 0.4rem 0.75rem;
      border-radius: 6px;
      font-size: 0.76rem;
      font-weight: 600;
      cursor: pointer;
    }

    .btn-save {
      border: 1px solid rgba(139, 156, 179, 0.35);
      background: #3d4f6f;
      color: var(--text-primary, #e8eaed);
    }

    .btn-clear {
      border: 1px solid var(--border-subtle, #252b36);
      background: transparent;
      color: var(--text-secondary, #a8b0bd);
    }

    .key-preview {
      margin: 0.55rem 0 0;
      font-size: 0.68rem;
      color: var(--text-muted, #6b7280);
    }
  `]
})
export class ApiKeySettingsComponent {
  readonly apiKeyService = inject(ApiKeyService);
  private readonly host = inject(ElementRef);

  open = false;
  draftKey = '';
  savedMessage = '';

  togglePanel(event: Event): void {
    event.stopPropagation();
    this.open = !this.open;
    if (this.open) {
      this.draftKey = this.apiKeyService.getKey();
      this.savedMessage = '';
    }
  }

  save(): void {
    this.apiKeyService.saveKey(this.draftKey);
    this.savedMessage = this.apiKeyService.hasKey() ? 'API key saved.' : 'API key removed.';
    this.draftKey = this.apiKeyService.getKey();
  }

  clear(): void {
    this.apiKeyService.clearKey();
    this.draftKey = '';
    this.savedMessage = 'API key removed.';
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.open) {
      return;
    }
    if (!this.host.nativeElement.contains(event.target)) {
      this.open = false;
    }
  }
}
