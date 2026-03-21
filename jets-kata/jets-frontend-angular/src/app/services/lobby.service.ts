import { Injectable, inject, signal, computed } from '@angular/core';
import { WebSocketService } from './websocket.service';
import {
  ConnectedData,
  ErrorData,
  GameMode,
  GameStartingData,
  LobbyStateData,
} from '../models/messages';

@Injectable({ providedIn: 'root' })
export class LobbyService {
  private ws = inject(WebSocketService);

  readonly playerId = signal<string | null>(null);
  readonly playerName = signal<string>('');
  readonly lobbyState = signal<LobbyStateData | null>(null);
  readonly error = signal<ErrorData | null>(null);
  readonly gameStarting = signal<GameStartingData | null>(null);

  readonly isHost = computed(() => {
    const lobby = this.lobbyState();
    const id = this.playerId();
    return lobby !== null && id !== null && lobby.hostId === id;
  });

  readonly myPlayer = computed(() => {
    const lobby = this.lobbyState();
    const id = this.playerId();
    if (!lobby || !id) return null;
    return lobby.players.find((p) => p.id === id) ?? null;
  });

  constructor() {
    this.ws.messages$.subscribe((msg) => {
      switch (msg.type) {
        case 'CONNECTED':
          this.playerId.set((msg.data as ConnectedData).playerId);
          break;
        case 'LOBBY_STATE':
          this.lobbyState.set(msg.data as LobbyStateData);
          this.error.set(null);
          break;
        case 'ERROR':
          this.error.set(msg.data as ErrorData);
          break;
        case 'GAME_STARTING':
          this.gameStarting.set(msg.data as GameStartingData);
          break;
      }
    });
  }

  createLobby(playerName: string): void {
    this.ws.send('CREATE_LOBBY', { playerName });
  }

  joinLobby(lobbyCode: string, playerName: string): void {
    this.ws.send('JOIN_LOBBY', { lobbyCode, playerName });
  }

  setReady(ready: boolean): void {
    this.ws.send('PLAYER_READY', { ready });
  }

  setGameMode(gameMode: GameMode): void {
    this.ws.send('SET_GAME_MODE', { gameMode });
  }

  startGame(): void {
    this.ws.send('START_GAME', {});
  }

  leaveLobby(): void {
    this.ws.send('LEAVE_LOBBY', {});
    this.lobbyState.set(null);
  }

  reset(): void {
    this.lobbyState.set(null);
    this.gameStarting.set(null);
    this.error.set(null);
  }
}
