import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CharacterApiService } from '../../core/services/character-api.service';
import {
  CreatePersonaRequest,
  CreateStoryRequest,
  PersonaApiService,
  StoryApiService
} from '../../core/services/roleplay-setup-api.service';
import { CreateCharacterRequest } from '../../core/services/character-api.service';
import { ImageUploadFieldComponent } from '../../shared/image-upload-field/image-upload-field.component';
import { ApiKeySettingsComponent } from '../../shared/api-key-settings/api-key-settings.component';

type CreateType = 'story' | 'persona' | 'character';

@Component({
  selector: 'app-create-content',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ImageUploadFieldComponent, ApiKeySettingsComponent],
  templateUrl: './create-content.component.html',
  styleUrl: './create-content.component.scss'
})
export class CreateContentComponent implements OnInit {
  activeType: CreateType | null = null;
  worlds: string[] = [];
  saving = false;
  error = '';
  savedMessage = '';

  storyForm: CreateStoryRequest = {
    title: '',
    worldId: '',
    premise: '',
    openingNarrative: '',
    startingCharacterNames: [],
    startingLocation: '',
    storyRules: []
  };

  personaForm: CreatePersonaRequest = {
    name: '',
    worldId: '',
    description: '',
    personality: [],
    background: '',
    speakingStyle: '',
    imageUrl: ''
  };

  characterForm: CreateCharacterRequest = {
    worldId: '',
    name: '',
    background: '',
    speakingStyle: '',
    personality: [],
    openingMessage: '',
    imageUrl: ''
  };

  storyCharacterName = '';
  storyRuleInput = '';
  personaTraitInput = '';
  characterTraitInput = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
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

    this.route.paramMap.subscribe(params => {
      const type = params.get('type') as CreateType | null;
      if (type === 'story' || type === 'persona' || type === 'character') {
        this.activeType = type;
      } else {
        this.activeType = null;
      }
      this.error = '';
      this.savedMessage = '';
    });
  }

  pageTitle(): string {
    switch (this.activeType) {
      case 'story':
        return 'Create Story';
      case 'persona':
        return 'Create Persona';
      case 'character':
        return 'Create AI Character';
      default:
        return 'Create Content';
    }
  }

  addStoryCharacter(): void {
    const name = this.storyCharacterName.trim();
    if (!name || this.storyForm.startingCharacterNames?.includes(name)) {
      return;
    }
    this.storyForm.startingCharacterNames = [...(this.storyForm.startingCharacterNames ?? []), name];
    this.storyCharacterName = '';
  }

  removeStoryCharacter(name: string): void {
    this.storyForm.startingCharacterNames = (this.storyForm.startingCharacterNames ?? [])
      .filter((item: string) => item !== name);
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
    if (!this.activeType) {
      return;
    }

    this.error = '';
    this.savedMessage = '';
    this.saving = true;

    if (this.activeType === 'story') {
      this.storyApi.createStory({
        ...this.storyForm,
        title: this.storyForm.title.trim(),
        worldId: this.storyForm.worldId.trim()
      }).subscribe({
        next: () => this.onSaved('Story created successfully.'),
        error: (err: { error?: { error?: string } }) => this.onError(err)
      });
      return;
    }

    if (this.activeType === 'persona') {
      this.personaApi.createPersona({
        ...this.personaForm,
        name: this.personaForm.name.trim(),
        worldId: this.personaForm.worldId?.trim() || undefined,
        imageUrl: this.personaForm.imageUrl || undefined
      }).subscribe({
        next: () => this.onSaved('Persona created successfully.'),
        error: (err: { error?: { error?: string } }) => this.onError(err)
      });
      return;
    }

    this.characterApi.createCharacter({
      ...this.characterForm,
      name: this.characterForm.name.trim(),
      worldId: this.characterForm.worldId.trim(),
      imageUrl: this.characterForm.imageUrl || undefined
    }).subscribe({
      next: () => this.onSaved('AI character created successfully.'),
      error: (err: { error?: { error?: string } }) => this.onError(err)
    });
  }

  private onSaved(message: string): void {
    this.saving = false;
    this.savedMessage = message;
    setTimeout(() => this.router.navigate(['/']), 900);
  }

  private onError(err: { error?: { error?: string } }): void {
    this.saving = false;
    this.error = err.error?.error ?? 'Failed to save. Check your inputs and try again.';
  }
}
