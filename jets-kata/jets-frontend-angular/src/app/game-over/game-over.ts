import { Component, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { WebSocketService } from '../services/websocket.service';

@Component({
  selector: 'app-game-over',
  standalone: true,
  templateUrl: './game-over.html',
  styleUrl: './game-over.scss',
})
export class GameOver implements OnInit, OnDestroy {
  private sub?: Subscription;

  reason = signal('');
  finalScores = signal<any[]>([]);
  isHost = signal(false);

  reasonText = computed(() => {
    switch (this.reason()) {
      case 'TIME_UP': return 'TIME IS UP';
      case 'SCORE_REACHED': return 'SCORE LIMIT REACHED';
      case 'ALL_LEFT': return 'ALL PLAYERS LEFT';
      default: return 'GAME OVER';
    }
  });

  constructor(public ws: WebSocketService, private router: Router) {
    const nav = this.router.getCurrentNavigation();
    const data = nav?.extras?.state?.['data'];
    if (data) {
      this.reason.set(data.reason);
      this.finalScores.set(data.finalScores ?? []);
    }
  }

  ngOnInit() {
    this.sub = this.ws.messages$.subscribe(msg => {
      if (msg.type === 'LOBBY_STATE') {
        this.router.navigate(['/']);
      }
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  returnToLobby() {
    this.ws.send('RETURN_TO_LOBBY', {});
  }
}
