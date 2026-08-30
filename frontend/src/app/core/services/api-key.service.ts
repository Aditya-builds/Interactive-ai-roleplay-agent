import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'roleplay.llm.apiKey';

@Injectable({ providedIn: 'root' })
export class ApiKeyService {
  private readonly hasKeySignal = signal(this.readFromStorage());

  hasKey(): boolean {
    return this.hasKeySignal();
  }

  getKey(): string {
    if (typeof localStorage === 'undefined') {
      return '';
    }
    return localStorage.getItem(STORAGE_KEY)?.trim() ?? '';
  }

  saveKey(value: string): void {
    const trimmed = value.trim();
    if (!trimmed) {
      this.clearKey();
      return;
    }
    localStorage.setItem(STORAGE_KEY, trimmed);
    this.hasKeySignal.set(true);
  }

  clearKey(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.hasKeySignal.set(false);
  }

  maskedPreview(): string {
    const key = this.getKey();
    if (!key) {
      return '';
    }
    if (key.length <= 8) {
      return '••••••••';
    }
    return `${key.slice(0, 4)}…${key.slice(-4)}`;
  }

  private readFromStorage(): boolean {
    return !!this.getKey();
  }
}
