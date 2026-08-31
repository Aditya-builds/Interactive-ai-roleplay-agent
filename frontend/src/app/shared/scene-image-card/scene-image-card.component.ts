import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-scene-image-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './scene-image-card.component.html',
  styleUrl: './scene-image-card.component.scss'
})
export class SceneImageCardComponent {
  @Input({ required: true }) imageSrc!: string;
  @Input() caption = '';
}
