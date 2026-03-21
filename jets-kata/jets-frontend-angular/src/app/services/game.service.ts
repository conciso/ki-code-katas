import { Injectable, inject, signal } from '@angular/core';
import { WebSocketService } from './websocket.service';
import {
  GameEventData,
  GameOverData,
  GameStateData,
  PlayerInputData,
  WaveCompleteData,
} from '../models/messages';

export interface GameEffect {
  event: string;
  x: number;
  y: number;
  details: Record<string, unknown>;
  createdAt: number;
}

@Injectable({ providedIn: 'root' })
export class GameService {
  private ws = inject(WebSocketService);

  readonly gameState = signal<GameStateData | null>(null);
  readonly gameOver = signal<GameOverData | null>(null);
  readonly effects = signal<GameEffect[]>([]);
  readonly waveComplete = signal<WaveCompleteData | null>(null);
  readonly returnedToLobby = signal(false);

  private inputSeq = 0;

  constructor() {
    this.ws.messages$.subscribe((msg) => {
      switch (msg.type) {
        case 'GAME_STATE':
          this.gameState.set(msg.data as GameStateData);
          break;
        case 'GAME_EVENT': {
          const ev = msg.data as GameEventData;
          const effect: GameEffect = { ...ev, createdAt: Date.now() };
          this.effects.update((prev) => [...prev.slice(-30), effect]);
          break;
        }
        case 'WAVE_COMPLETE':
          this.waveComplete.set(msg.data as WaveCompleteData);
          setTimeout(() => this.waveComplete.set(null), 4000);
          break;
        case 'GAME_OVER':
          this.gameOver.set(msg.data as GameOverData);
          break;
        case 'LOBBY_STATE':
          if (this.gameOver()) {
            this.returnedToLobby.set(true);
          }
          break;
      }
    });
  }

  sendInput(input: Omit<PlayerInputData, 'seq'>): number {
    const seq = ++this.inputSeq;
    this.ws.send('PLAYER_INPUT', { ...input, seq });
    return seq;
  }

  returnToLobby(): void {
    this.ws.send('RETURN_TO_LOBBY', {});
  }

  reset(): void {
    this.gameState.set(null);
    this.gameOver.set(null);
    this.effects.set([]);
    this.waveComplete.set(null);
    this.returnedToLobby.set(false);
    this.inputSeq = 0;
  }
}
