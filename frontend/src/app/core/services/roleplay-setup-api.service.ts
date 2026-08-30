import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PlayerPersona, Story } from '../models/roleplay-setup.model';

@Injectable({ providedIn: 'root' })
export class PersonaApiService {
  private readonly baseUrl = '/api/personas';

  constructor(private http: HttpClient) {}

  listPersonas(): Observable<PlayerPersona[]> {
    return this.http.get<PlayerPersona[]>(this.baseUrl);
  }
}

@Injectable({ providedIn: 'root' })
export class StoryApiService {
  private readonly baseUrl = '/api/stories';

  constructor(private http: HttpClient) {}

  listStories(): Observable<Story[]> {
    return this.http.get<Story[]>(this.baseUrl);
  }
}
