import { Component, inject, signal, effect } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LobbyService } from '../../services/lobby.service';
import { GameService } from '../../services/game.service';
import { GameMode } from '../../models/messages';

@Component({
  selector: 'app-lobby',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './lobby.html',
  styleUrl: './lobby.scss',
})
export class LobbyComponent {
  readonly lobby = inject(LobbyService);
  private game = inject(GameService);
  private router = inject(Router);

  joinCode = signal('');
  view = signal<'menu' | 'lobby'>('menu');

  constructor() {
    effect(() => {
      if (this.lobby.gameStarting()) {
        this.game.reset();
        this.router.navigate(['/game']);
      }
    });

    if (this.lobby.lobbyState()) {
      this.view.set('lobby');
    }
  }

  createLobby(): void {
    const name = this.lobby.playerName();
    if (!name) return;
    this.lobby.createLobby(name);
    this.view.set('lobby');
  }

  joinLobby(): void {
    const code = this.joinCode().trim().toUpperCase();
    const name = this.lobby.playerName();
    if (!code || !name) return;
    this.lobby.joinLobby(code, name);
    this.view.set('lobby');
  }

  toggleReady(): void {
    const me = this.lobby.myPlayer();
    this.lobby.setReady(!me?.ready);
  }

  setGameMode(mode: GameMode): void {
    this.lobby.setGameMode(mode);
  }

  startGame(): void {
    this.lobby.startGame();
  }

  leave(): void {
    this.lobby.leaveLobby();
    this.view.set('menu');
  }

  get allReady(): boolean {
    const state = this.lobby.lobbyState();
    if (!state || state.players.length < 2) return false;
    return state.players.every((p) => p.ready);
  }
}
