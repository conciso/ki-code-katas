import { Injectable, signal, computed } from '@angular/core';
import { Subject } from 'rxjs';

export interface WsMessage {
  type: string;
  data: any;
}

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private ws: WebSocket | null = null;
  private readonly _messages$ = new Subject<WsMessage>();
  private readonly _playerId = signal<string | null>(null);
  private readonly _connectionState = signal<'disconnected' | 'connecting' | 'connected'>('disconnected');

  readonly messages$ = this._messages$.asObservable();
  readonly playerId = this._playerId.asReadonly();
  readonly connectionState = this._connectionState.asReadonly();
  readonly isConnected = computed(() => this._connectionState() === 'connected');

  connect(playerName: string, host: string = 'localhost:5075'): void {
    if (this.ws) {
      this.ws.close();
    }

    this._connectionState.set('connecting');
    this.ws = new WebSocket(`ws://${host}/ws/game?playerName=${encodeURIComponent(playerName)}`);

    this.ws.onopen = () => {
      this._connectionState.set('connected');
    };

    this.ws.onmessage = (event) => {
      const msg: WsMessage = JSON.parse(event.data);

      if (msg.type === 'CONNECTED') {
        this._playerId.set(msg.data.playerId);
      }

      this._messages$.next(msg);
    };

    this.ws.onclose = () => {
      this._connectionState.set('disconnected');
      this._playerId.set(null);
    };

    this.ws.onerror = () => {
      this._connectionState.set('disconnected');
    };
  }

  send(type: string, data: any = {}): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ type, data }));
    }
  }

  disconnect(): void {
    this.ws?.close();
    this.ws = null;
  }
}
