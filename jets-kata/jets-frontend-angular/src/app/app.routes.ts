import { Routes } from '@angular/router';
import { ConnectComponent } from './components/connect/connect';
import { LobbyComponent } from './components/lobby/lobby';
import { GameComponent } from './components/game/game';
import { GameOverComponent } from './components/game-over/game-over';

export const routes: Routes = [
  { path: '', component: ConnectComponent },
  { path: 'lobby', component: LobbyComponent },
  { path: 'game', component: GameComponent },
  { path: 'game-over', component: GameOverComponent },
  { path: '**', redirectTo: '' },
];
