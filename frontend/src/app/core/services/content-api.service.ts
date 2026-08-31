import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../config/api-url';

export type ContentImageKind = 'personas' | 'characters';

@Injectable({ providedIn: 'root' })
export class ContentApiService {
  private readonly baseUrl = apiUrl('/api/content');

  constructor(private http: HttpClient) {}

  uploadImage(file: File, kind: ContentImageKind): Observable<{ imageUrl: string }> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('kind', kind);
    return this.http.post<{ imageUrl: string }>(`${this.baseUrl}/images`, formData);
  }
}
