import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { ChatComponent } from './chat.component';
import { Conversation, SendMessageResponse } from '../../core/models/conversation.model';

describe('ChatComponent state panel sync', () => {
  let fixture: ComponentFixture<ChatComponent>;
  let component: ChatComponent;
  let http: HttpTestingController;
  const conversationId = 'test-conv-id';

  const initialConversation: Conversation = {
    id: conversationId,
    characterId: 'aurora',
    worldId: 'fantasy',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    characterState: {
      characterId: 'aurora',
      health: { current: 100, max: 100 },
      status: null,
      emotion: null
    },
    scene: {
      location: 'guild_hall',
      userLocation: 'guild_hall',
      time: 'evening',
      charactersPresent: ['aurora', 'user'],
      currentSituation: 'Aurora reviews reports at her desk.',
      currentConflict: null
    },
    relationships: [{
      targetId: 'user',
      trust: 42,
      respect: 67,
      affection: 12,
      familiarity: 54,
      suspicion: 8
    }],
    memories: [],
    messages: [{
      id: 'm1',
      role: 'assistant',
      content: 'You finally came.',
      timestamp: '2026-01-01T00:00:00Z'
    }]
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChatComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(convertToParamMap({ conversationId })) }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ChatComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  function flushInitialLoad(): void {
    const convReq = http.expectOne(`/api/conversations/${conversationId}`);
    convReq.flush(initialConversation);
    const charReq = http.expectOne('/api/characters/aurora');
    charReq.flush({
      character: { id: 'aurora', worldId: 'fantasy', name: 'Aurora', personality: [], background: '', speakingStyle: '', values: [] },
      world: { id: 'fantasy', name: 'Fantasy', description: '', rules: [] }
    });
    fixture.detectChanges();
  }

  it('loads initial state into the panel', fakeAsync(() => {
    fixture.detectChanges();
    flushInitialLoad();
    tick();

    expect(component.scene?.location).toBe('guild_hall');
    expect(component.characterState?.health.current).toBe(100);
    expect(component.relationships[0].trust).toBe(42);
  }));

  it('updates state panel after send via conversation reload', fakeAsync(() => {
    fixture.detectChanges();
    flushInitialLoad();
    tick();

    component.onSend('you look tired are you alright ?');
    fixture.detectChanges();

    const sendReq = http.expectOne(`/api/conversations/${conversationId}/messages`);
    expect(sendReq.request.body).toEqual({ content: 'you look tired are you alright ?' });
    sendReq.flush({
      message: { id: 'm3', role: 'assistant', content: 'I appreciate you asking.', timestamp: '2026-01-01T00:01:00Z' },
      conversationId,
      scene: initialConversation.scene,
      characterState: initialConversation.characterState,
      relationships: initialConversation.relationships
    } satisfies SendMessageResponse);

    const reloadReq = http.expectOne(`/api/conversations/${conversationId}`);
    reloadReq.flush({
      ...initialConversation,
      characterState: {
        characterId: 'aurora',
        health: { current: 100, max: 100 },
        status: 'exhausted',
        emotion: 'grateful'
      },
      relationships: [{
        targetId: 'user',
        trust: 44,
        respect: 67,
        affection: 13,
        familiarity: 54,
        suspicion: 8
      }],
      scene: {
        ...initialConversation.scene,
        currentSituation: 'Aurora is opening up about her fatigue.'
      },
      messages: [
        ...initialConversation.messages,
        { id: 'm2', role: 'user', content: 'you look tired are you alright ?', timestamp: '2026-01-01T00:00:30Z' },
        { id: 'm3', role: 'assistant', content: 'I appreciate you asking.', timestamp: '2026-01-01T00:01:00Z' }
      ]
    });

    tick();
    fixture.detectChanges();

    expect(component.characterState?.emotion).toBe('grateful');
    expect(component.characterState?.status).toBe('exhausted');
    expect(component.relationships[0].trust).toBe(44);
    expect(component.scene?.currentSituation).toBe('Aurora is opening up about her fatigue.');
    expect(component.messages.length).toBe(3);
  }));
});
