import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CharacterApiService } from '../../../../core/services/character-api.service';
import {
  CreatePersonaRequest,
  CreateStoryRequest,
  PersonaApiService,
  StoryApiService
} from '../../../../core/services/roleplay-setup-api.service';
import { CreateCharacterRequest } from '../../../../core/services/character-api.service';

type CreateTab = 'story' | 'persona' | 'character';

@Component({
  selector: 'app-content-create-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './content-create-panel.component.html',
  styleUrl: './content-create-panel.component.scss'
})
export class ContentCreatePanelComponent implements OnInit {
  @Input() characterIds: string[] = [];
  @Output() created = new EventEmitter<void>();

  activeTab: CreateTab = 'story';
  worlds: string[] = [];
  saving = false;
  error = '';

  storyForm: CreateStoryRequest = {
    id: '',
    title: '',
    worldId: '',
    premise: '',
    openingNarrative: '',
    startingCharacters: [],
    startingLocation: '',
    storyRules: []
  };

  personaForm: CreatePersonaRequest = {
    id: '',
    name: '',
    worldId: '',
    description: '',
    personality: [],
    background: '',
    speakingStyle: ''
  };

  characterForm: CreateCharacterRequest = {
    id: '',
    worldId: '',
    name: '',
    background: '',
    speakingStyle: '',
    personality: [],
    openingMessage: ''
  };

  storyStartingCharacterId = '';
  storyRuleInput = '';
  personaTraitInput = '';
  characterTraitInput = '';

  constructor(
    private storyApi: StoryApiService,
    private personaApi: PersonaApiService,
    private characterApi: CharacterApiService
  ) {}

  ngOnInit(): void {
    this.characterApi.listWorlds().subscribe({
      next: (worlds: string[]) => {
        this.worlds = worlds;
        if (worlds.length > 0) {
          this.storyForm.worldId = worlds[0];
          this.personaForm.worldId = worlds[0];
          this.characterForm.worldId = worlds[0];
        }
      }
    });
  }

  setTab(tab: CreateTab): void {
    this.activeTab = tab;
    this.error = '';
  }

  addStoryCharacter(): void {
    const id = this.storyStartingCharacterId.trim();
    if (!id || this.storyForm.startingCharacters?.includes(id)) {
      return;
    }
    this.storyForm.startingCharacters = [...(this.storyForm.startingCharacters ?? []), id];
    this.storyStartingCharacterId = '';
  }

  removeStoryCharacter(id: string): void {
    this.storyForm.startingCharacters = (this.storyForm.startingCharacters ?? []).filter((item: string) => item !== id);
  }

  addStoryRule(): void {
    const rule = this.storyRuleInput.trim();
    if (!rule) {
      return;
    }
    this.storyForm.storyRules = [...(this.storyForm.storyRules ?? []), rule];
    this.storyRuleInput = '';
  }

  addPersonaTrait(): void {
    const trait = this.personaTraitInput.trim();
    if (!trait) {
      return;
    }
    this.personaForm.personality = [...(this.personaForm.personality ?? []), trait];
    this.personaTraitInput = '';
  }

  addCharacterTrait(): void {
    const trait = this.characterTraitInput.trim();
    if (!trait) {
      return;
    }
    this.characterForm.personality = [...(this.characterForm.personality ?? []), trait];
    this.characterTraitInput = '';
  }

  save(): void {
    this.error = '';
    this.saving = true;

    if (this.activeTab === 'story') {
      this.storyApi.createStory({
        ...this.storyForm,
        id: this.storyForm.id.trim(),
        title: this.storyForm.title.trim(),
        worldId: this.storyForm.worldId.trim()
      }).subscribe({
        next: () => this.onSaved(),
        error: (err: { error?: { error?: string } }) => this.onError(err)
      });
      return;
    }

    if (this.activeTab === 'persona') {
      this.personaApi.createPersona({
        ...this.personaForm,
        id: this.personaForm.id.trim(),
        name: this.personaForm.name.trim(),
        worldId: this.personaForm.worldId?.trim() || undefined
      }).subscribe({
        next: () => this.onSaved(),
        error: (err: { error?: { error?: string } }) => this.onError(err)
      });
      return;
    }

    this.characterApi.createCharacter({
      ...this.characterForm,
      id: this.characterForm.id.trim(),
      name: this.characterForm.name.trim(),
      worldId: this.characterForm.worldId.trim()
    }).subscribe({
      next: () => this.onSaved(),
      error: (err: { error?: { error?: string } }) => this.onError(err)
    });
  }

  private onSaved(): void {
    this.saving = false;
    this.created.emit();
  }

  private onError(err: { error?: { error?: string } }): void {
    this.saving = false;
    this.error = err.error?.error ?? 'Failed to save. Check your inputs and try again.';
  }
}
