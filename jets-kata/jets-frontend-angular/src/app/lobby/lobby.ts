import { Component, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { WebSocketService } from '../services/websocket.service';

@Component({
  selector: 'app-lobby',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './lobby.html',
  styleUrl: './lobby.scss',
})
export class Lobby implements OnInit, OnDestroy {
  private sub?: Subscription;

  playerName = signal('');
  lobbyCode = signal('');
  joinCode = signal('');
  errorMessage = signal('');

  inLobby = signal(false);
  hostId = signal('');
  players = signal<any[]>([]);
  gameMode = signal('FFA');

  isHost = computed(() => this.ws.playerId() === this.hostId());
  allReady = computed(() => this.players().length >= 2 && this.players().every(p => p.ready));
  isReady = computed(() => {
    const me = this.players().find(p => p.id === this.ws.playerId());
    return me?.ready ?? false;
  });

  constructor(public ws: WebSocketService, private router: Router) {}

  ngOnInit() {
    this.sub = this.ws.messages$.subscribe(msg => {
      switch (msg.type) {
        case 'LOBBY_CREATED':
          this.lobbyCode.set(msg.data.lobbyCode);
          this.hostId.set(msg.data.hostId);
          this.inLobby.set(true);
          this.errorMessage.set('');
          break;
        case 'LOBBY_STATE':
          this.lobbyCode.set(msg.data.lobbyCode);
          this.hostId.set(msg.data.hostId);
          this.players.set(msg.data.players);
          this.gameMode.set(msg.data.gameMode);
          this.inLobby.set(true);
          this.errorMessage.set('');
          break;
        case 'GAME_STARTING':
          this.router.navigate(['/game']);
          break;
        case 'ERROR':
          this.errorMessage.set(msg.data.message);
          break;
      }
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  connect() {
    if (!this.playerName()) return;
    this.ws.connect(this.playerName());
  }

  createLobby() {
    this.ws.send('CREATE_LOBBY', { playerName: this.playerName() });
  }

  joinLobby() {
    if (!this.joinCode()) return;
    this.ws.send('JOIN_LOBBY', { lobbyCode: this.joinCode().toUpperCase(), playerName: this.playerName() });
  }

  toggleReady() {
    this.ws.send('PLAYER_READY', { ready: !this.isReady() });
  }

  startGame() {
    this.ws.send('START_GAME', {});
  }

  leaveLobby() {
    this.ws.send('LEAVE_LOBBY', {});
    this.inLobby.set(false);
    this.players.set([]);
  }
}
