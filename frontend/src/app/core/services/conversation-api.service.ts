import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Conversation,
  ConversationSummary,
  ReplyLength,
  SendMessageResponse
} from '../models/conversation.model';
import { CreateConversationRequest } from '../models/roleplay-setup.model';
import { apiUrl } from '../config/api-url';

@Injectable({ providedIn: 'root' })
export class ConversationApiService {
  private readonly baseUrl = apiUrl('/api/conversations');

  constructor(private http: HttpClient) {}

  listConversations(characterId?: string): Observable<ConversationSummary[]> {
    let params = new HttpParams();
    if (characterId) {
      params = params.set('characterId', characterId);
    }
    return this.http.get<ConversationSummary[]>(this.baseUrl, { params });
  }

  createConversation(request: CreateConversationRequest): Observable<Conversation> {
    return this.http.post<Conversation>(this.baseUrl, request);
  }

  createLegacyConversation(characterId: string): Observable<Conversation> {
    return this.createConversation({ characterId });
  }

  getConversation(id: string): Observable<Conversation> {
    return this.http.get<Conversation>(`${this.baseUrl}/${id}`);
  }

  sendMessage(
    conversationId: string,
    content: string,
    replyLength: ReplyLength = 'normal'
  ): Observable<SendMessageResponse> {
    return this.http.post<SendMessageResponse>(`${this.baseUrl}/${conversationId}/messages`, {
      content,
      replyLength
    });
  }

  regenerateMessage(
    conversationId: string,
    replyLength: ReplyLength = 'normal'
  ): Observable<SendMessageResponse> {
    return this.http.post<SendMessageResponse>(`${this.baseUrl}/${conversationId}/messages/regenerate`, {
      replyLength
    });
  }

  deleteConversation(conversationId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${conversationId}`);
  }
}
