import { Injectable, signal } from '@angular/core';
import { Subject } from 'rxjs';
import { MessageType, WsMessage } from '../models/messages';

export type ConnectionState = 'disconnected' | 'connecting' | 'connected' | 'error';

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private ws: WebSocket | null = null;

  readonly connectionState = signal<ConnectionState>('disconnected');
  readonly messages$ = new Subject<WsMessage>();

  connect(host: string, port: number, playerName: string): void {
    if (this.ws) {
      this.ws.close();
    }

    this.connectionState.set('connecting'); // setzt auch einen vorherigen Fehler zurück
    const url = `ws://${host}:${port}/ws/game?playerName=${encodeURIComponent(playerName)}`;
    this.ws = new WebSocket(url);

    this.ws.onopen = () => {
      this.connectionState.set('connected');
    };

    this.ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data) as WsMessage;
        this.messages$.next(message);
      } catch {
        console.error('Failed to parse WebSocket message', event.data);
      }
    };

    this.ws.onerror = () => {
      this.connectionState.set('error');
    };

    this.ws.onclose = () => {
      // Fehler-State nicht überschreiben — der User soll die Fehlermeldung sehen
      if (this.connectionState() !== 'error') {
        this.connectionState.set('disconnected');
      }
    };
  }

  send<T>(type: MessageType, data: T): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ type, data }));
    }
  }

  disconnect(): void {
    this.ws?.close();
    this.ws = null;
  }
}
