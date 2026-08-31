import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../config/api-url';
import { GenerateSceneImageResponse } from '../models/scene-image.model';

@Injectable({ providedIn: 'root' })
export class SceneImageApiService {
  constructor(private http: HttpClient) {}

  generateSceneImage(conversationId: string): Observable<GenerateSceneImageResponse> {
    return this.http.post<GenerateSceneImageResponse>(
      apiUrl(`/api/conversations/${conversationId}/scene-images`),
      {}
    );
  }

  sceneImageContentUrl(sceneImageId: string): string {
    return apiUrl(`/api/scene-images/${sceneImageId}/content`);
  }
}
