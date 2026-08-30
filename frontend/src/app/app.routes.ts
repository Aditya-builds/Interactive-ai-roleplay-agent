import { Routes } from '@angular/router';
import { CharacterSelectComponent } from './features/character-select/character-select.component';
import { ChatComponent } from './features/chat/chat.component';

export const routes: Routes = [
  { path: '', component: CharacterSelectComponent },
  { path: 'chat/:conversationId', component: ChatComponent },
  { path: '**', redirectTo: '' }
];
