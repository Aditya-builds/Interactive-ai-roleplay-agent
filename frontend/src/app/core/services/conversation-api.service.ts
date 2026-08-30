import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Conversation, SendMessageResponse } from '../models/conversation.model';
import { CreateConversationRequest } from '../models/roleplay-setup.model';

@Injectable({ providedIn: 'root' })
export class ConversationApiService {
  private readonly baseUrl = '/api/conversations';

  constructor(private http: HttpClient) {}

  createConversation(request: CreateConversationRequest): Observable<Conversation> {
    return this.http.post<Conversation>(this.baseUrl, request);
  }

  /** @deprecated Use createConversation with CreateConversationRequest */
  createLegacyConversation(characterId: string): Observable<Conversation> {
    return this.createConversation({ characterId });
  }

  getConversation(id: string): Observable<Conversation> {
    return this.http.get<Conversation>(`${this.baseUrl}/${id}`);
  }

  sendMessage(conversationId: string, content: string): Observable<SendMessageResponse> {
    return this.http.post<SendMessageResponse>(`${this.baseUrl}/${conversationId}/messages`, { content });
  }

  deleteConversation(conversationId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${conversationId}`);
  }
}
