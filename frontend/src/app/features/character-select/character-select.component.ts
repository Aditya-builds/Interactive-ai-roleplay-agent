import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { CharacterApiService } from '../../core/services/character-api.service';
import { ConversationApiService } from '../../core/services/conversation-api.service';
import { RoleplayCharacter } from '../../core/models/character.model';
import { LoadingSpinnerComponent } from '../../shared/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-character-select',
  standalone: true,
  imports: [CommonModule, LoadingSpinnerComponent],
  templateUrl: './character-select.component.html',
  styleUrl: './character-select.component.scss'
})
export class CharacterSelectComponent implements OnInit {
  characters: RoleplayCharacter[] = [];
  loading = true;
  starting = false;
  error = '';

  constructor(
    private characterApi: CharacterApiService,
    private conversationApi: ConversationApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.characterApi.listCharacters().subscribe({
      next: (characters) => {
        this.characters = characters;
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = err.error?.error ?? 'Failed to load characters.';
      }
    });
  }

  selectCharacter(character: RoleplayCharacter): void {
    if (this.starting) {
      return;
    }

    this.starting = true;
    this.error = '';

    this.conversationApi.createConversation(character.id).subscribe({
      next: (conversation) => {
        this.router.navigate(['/chat', conversation.id]);
      },
      error: (err: HttpErrorResponse) => {
        this.starting = false;
        this.error = err.error?.error ?? 'Failed to start conversation.';
      }
    });
  }
}
