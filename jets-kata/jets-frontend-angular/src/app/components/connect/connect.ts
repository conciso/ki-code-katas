import { Component, inject, signal, effect } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { WebSocketService } from '../../services/websocket.service';
import { LobbyService } from '../../services/lobby.service';

@Component({
  selector: 'app-connect',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './connect.html',
  styleUrl: './connect.scss',
})
export class ConnectComponent {
  private ws = inject(WebSocketService);
  private lobby = inject(LobbyService);
  private router = inject(Router);

  playerName = signal('');
  host = signal('localhost');
  port = signal(8080);

  readonly connectionState = this.ws.connectionState;

  constructor() {
    effect(() => {
      if (this.ws.connectionState() === 'connected' && this.lobby.playerId()) {
        this.router.navigate(['/lobby']);
      }
    });
  }

  connect(): void {
    const name = this.playerName().trim();
    if (!name) return;
    this.lobby.playerName.set(name);
    this.ws.connect(this.host(), this.port(), name);
  }
}
