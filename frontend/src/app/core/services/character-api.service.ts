import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CharacterDetailResponse, RoleplayCharacter } from '../models/character.model';
import { apiUrl } from '../config/api-url';

@Injectable({ providedIn: 'root' })
export class CharacterApiService {
  private readonly baseUrl = apiUrl('/api/characters');

  constructor(private http: HttpClient) {}

  listCharacters(): Observable<RoleplayCharacter[]> {
    return this.http.get<RoleplayCharacter[]>(this.baseUrl);
  }

  getCharacter(id: string): Observable<CharacterDetailResponse> {
    return this.http.get<CharacterDetailResponse>(`${this.baseUrl}/${id}`);
  }
}
