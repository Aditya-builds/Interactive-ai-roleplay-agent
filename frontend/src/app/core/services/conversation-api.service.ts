import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Conversation, SendMessageResponse } from '../models/conversation.model';

@Injectable({ providedIn: 'root' })
export class ConversationApiService {
  private readonly baseUrl = '/api/conversations';

  constructor(private http: HttpClient) {}

  createConversation(characterId: string): Observable<Conversation> {
    return this.http.post<Conversation>(this.baseUrl, { characterId });
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
