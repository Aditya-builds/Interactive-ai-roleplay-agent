import { Routes } from '@angular/router';
import { CharacterSelectionComponent } from './features/character-selection/character-selection.component';
import { ChatComponent } from './features/chat/chat.component';
import { CreateContentComponent } from './features/create-content/create-content.component';

export const routes: Routes = [
  { path: '', component: CharacterSelectionComponent },
  { path: 'create', component: CreateContentComponent },
  { path: 'create/:type', component: CreateContentComponent },
  { path: 'chat/:conversationId', component: ChatComponent },
  { path: '**', redirectTo: '' }
];
