import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CharacterDetailResponse, RoleplayCharacter } from '../models/character.model';
import { apiUrl } from '../config/api-url';

export interface CreateCharacterRequest {
  id?: string;
  worldId: string;
  name: string;
  background?: string;
  speakingStyle?: string;
  personality?: string[];
  openingMessage?: string;
  imageUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class CharacterApiService {
  private readonly baseUrl = apiUrl('/api/characters');

  constructor(private http: HttpClient) {}

  listCharacters(): Observable<RoleplayCharacter[]> {
    return this.http.get<RoleplayCharacter[]>(this.baseUrl);
  }

  listWorlds(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/worlds`);
  }

  createCharacter(request: CreateCharacterRequest): Observable<RoleplayCharacter> {
    return this.http.post<RoleplayCharacter>(this.baseUrl, request);
  }

  getCharacter(id: string): Observable<CharacterDetailResponse> {
    return this.http.get<CharacterDetailResponse>(`${this.baseUrl}/${id}`);
  }
}
