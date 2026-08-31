import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ContentApiService, ContentImageKind } from '../../core/services/content-api.service';
import { resolveActorImageUrl } from '../../core/config/api-url';

@Component({
  selector: 'app-image-upload-field',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './image-upload-field.component.html',
  styleUrl: './image-upload-field.component.scss'
})
export class ImageUploadFieldComponent {
  @Input() label = 'Portrait image';
  @Input() kind: ContentImageKind = 'characters';
  @Input() imageUrl: string | undefined = '';
  @Output() imageUrlChange = new EventEmitter<string>();

  uploading = false;
  error = '';
  previewUrl = '';

  constructor(private contentApi: ContentApiService) {}

  get displayUrl(): string {
    if (this.previewUrl) {
      return this.previewUrl;
    }
    return resolveActorImageUrl(this.imageUrl ?? '');
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    this.previewUrl = URL.createObjectURL(file);
    this.uploading = true;
    this.error = '';

    this.contentApi.uploadImage(file, this.kind).subscribe({
      next: response => {
        this.imageUrl = response.imageUrl;
        this.imageUrlChange.emit(response.imageUrl);
        this.uploading = false;
      },
      error: () => {
        this.uploading = false;
        this.error = 'Image upload failed. Try a JPG or PNG under 5MB.';
      }
    });
  }

  clearImage(): void {
    this.previewUrl = '';
    this.imageUrl = '';
    this.imageUrlChange.emit('');
  }
}
