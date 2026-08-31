import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlayerPersona } from '../../../../core/models/roleplay-setup.model';
import { ActorPortraitComponent } from '../../../../shared/actor-portrait/actor-portrait.component';
import { resolveActorImageUrl } from '../../../../core/config/api-url';

@Component({
  selector: 'app-persona-pick-card',
  standalone: true,
  imports: [CommonModule, ActorPortraitComponent],
  templateUrl: './persona-pick-card.component.html',
  styleUrl: './persona-pick-card.component.scss'
})
export class PersonaPickCardComponent {
  @Input({ required: true }) persona!: PlayerPersona;
  @Input() selected = false;
  @Output() selectPersona = new EventEmitter<PlayerPersona>();

  actorImageUrl(url?: string | null): string {
    return resolveActorImageUrl(url);
  }

  traitSummary(): string {
    const traits = this.persona.profile?.personality ?? [];
    return traits.slice(0, 3).join(' · ');
  }
}
