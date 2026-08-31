import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SceneImageApiService } from '../../core/services/scene-image-api.service';
import { GeneratedSceneImage } from '../../core/models/scene-image.model';

@Component({
  selector: 'app-scene-image-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './scene-image-card.component.html',
  styleUrl: './scene-image-card.component.scss'
})
export class SceneImageCardComponent implements OnChanges {
  private readonly sceneImageApi = inject(SceneImageApiService);

  @Input({ required: true }) imageSrc!: string;
  @Input() caption = '';
  @Input() sceneImageId = '';
  @Input() showDebug = false;

  debugMetadata: GeneratedSceneImage | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (this.showDebug && this.sceneImageId && (changes['sceneImageId'] || changes['showDebug'])) {
      this.sceneImageApi.getSceneImageMetadata(this.sceneImageId).subscribe({
        next: (metadata) => {
          this.debugMetadata = metadata;
        }
      });
    }
  }
}
