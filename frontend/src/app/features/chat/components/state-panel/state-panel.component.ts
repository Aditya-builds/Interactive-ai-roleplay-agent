import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  CharacterRuntimeState,
  Relationship,
  Scene,
  formatEmotionLabel,
  formatLocationSlug,
  formatPresentName,
  formatStatusLabel,
  formatTimeSlug,
  userRelationship
} from '../../../../core/models/conversation.model';

@Component({
  selector: 'app-state-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './state-panel.component.html',
  styleUrl: './state-panel.component.scss'
})
export class StatePanelComponent {
  @Input({ required: true }) scene!: Scene;
  @Input({ required: true }) characterName!: string;
  @Input() characterState: CharacterRuntimeState | null = null;
  @Input() relationships: Relationship[] = [];

  formatLocation = formatLocationSlug;
  formatTime = formatTimeSlug;
  formatStatus = formatStatusLabel;
  formatEmotion = formatEmotionLabel;
  formatPresent = formatPresentName;

  get userRel(): Relationship | undefined {
    return userRelationship(this.relationships);
  }

  get npcLocation(): string {
    return formatLocationSlug(this.scene.location);
  }

  get userLocationLabel(): string | null {
    const userLoc = this.scene.userLocation ?? this.scene.location;
    if (userLoc !== this.scene.location) {
      return formatLocationSlug(userLoc);
    }
    return null;
  }

  get presentNames(): string[] {
    return this.scene.charactersPresent.map(id => formatPresentName(id, this.characterName));
  }
}
