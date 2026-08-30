import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { CharacterApiService } from '../../core/services/character-api.service';
import { ConversationApiService } from '../../core/services/conversation-api.service';
import { RoleplayCharacter } from '../../core/models/character.model';
import { LoadingSpinnerComponent } from '../../shared/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-character-selection',
  standalone: true,
  imports: [CommonModule, LoadingSpinnerComponent],
  templateUrl: './character-selection.component.html',
  styleUrl: './character-selection.component.scss'
})
export class CharacterSelectionComponent implements OnInit {
  characters: RoleplayCharacter[] = [];
  loading = true;
  starting = false;
  loadError = false;

  constructor(
    private characterApi: CharacterApiService,
    private conversationApi: ConversationApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCharacters();
  }

  loadCharacters(): void {
    this.loading = true;
    this.loadError = false;

    this.characterApi.listCharacters().subscribe({
      next: (characters) => {
        this.characters = characters;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.loadError = true;
      }
    });
  }

  startConversation(character: RoleplayCharacter): void {
    if (this.starting) {
      return;
    }

    this.starting = true;
    this.loadError = false;

    this.conversationApi.createConversation(character.id).subscribe({
      next: (conversation) => {
        this.router.navigate(['/chat', conversation.id]);
      },
      error: () => {
        this.starting = false;
        this.loadError = true;
      }
    });
  }

  personalitySummary(character: RoleplayCharacter): string {
    return character.personality.slice(0, 3).join(' · ');
  }
}
