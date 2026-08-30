import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CharacterDetailResponse, RoleplayCharacter } from '../models/character.model';

@Injectable({ providedIn: 'root' })
export class CharacterApiService {
  private readonly baseUrl = '/api/characters';

  constructor(private http: HttpClient) {}

  listCharacters(): Observable<RoleplayCharacter[]> {
    return this.http.get<RoleplayCharacter[]>(this.baseUrl);
  }

  getCharacter(id: string): Observable<CharacterDetailResponse> {
    return this.http.get<CharacterDetailResponse>(`${this.baseUrl}/${id}`);
  }
}
