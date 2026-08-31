import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PlayerPersona, Story } from '../models/roleplay-setup.model';
import { apiUrl } from '../config/api-url';

export interface CreatePersonaRequest {
  id?: string;
  name: string;
  worldId?: string;
  description?: string;
  personality?: string[];
  background?: string;
  speakingStyle?: string;
  imageUrl?: string;
}

export interface CreateStoryRequest {
  id?: string;
  title: string;
  worldId: string;
  premise?: string;
  openingNarrative?: string;
  startingCharacters?: string[];
  startingCharacterNames?: string[];
  startingLocation?: string;
  storyRules?: string[];
}

@Injectable({ providedIn: 'root' })
export class PersonaApiService {
  private readonly baseUrl = apiUrl('/api/personas');

  constructor(private http: HttpClient) {}

  listPersonas(): Observable<PlayerPersona[]> {
    return this.http.get<PlayerPersona[]>(this.baseUrl);
  }

  getPersona(id: string): Observable<PlayerPersona> {
    return this.http.get<PlayerPersona>(`${this.baseUrl}/${id}`);
  }

  createPersona(request: CreatePersonaRequest): Observable<PlayerPersona> {
    return this.http.post<PlayerPersona>(this.baseUrl, request);
  }
}

@Injectable({ providedIn: 'root' })
export class StoryApiService {
  private readonly baseUrl = apiUrl('/api/stories');

  constructor(private http: HttpClient) {}

  listStories(): Observable<Story[]> {
    return this.http.get<Story[]>(this.baseUrl);
  }

  createStory(request: CreateStoryRequest): Observable<Story> {
    return this.http.post<Story>(this.baseUrl, request);
  }
}
