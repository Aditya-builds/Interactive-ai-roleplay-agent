import { Routes } from '@angular/router';
import { CharacterSelectionComponent } from './features/character-selection/character-selection.component';
import { ChatComponent } from './features/chat/chat.component';

export const routes: Routes = [
  { path: '', component: CharacterSelectionComponent },
  { path: 'chat/:conversationId', component: ChatComponent },
  { path: '**', redirectTo: '' }
];
