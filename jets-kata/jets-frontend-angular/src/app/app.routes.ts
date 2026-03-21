import { Routes } from '@angular/router';
import { Lobby } from './lobby/lobby';
import { Game } from './game/game';
import { GameOver } from './game-over/game-over';

export const routes: Routes = [
  { path: '', component: Lobby },
  { path: 'game', component: Game },
  { path: 'game-over', component: GameOver },
];
