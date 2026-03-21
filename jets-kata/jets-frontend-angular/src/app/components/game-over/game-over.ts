import { Component, inject, effect } from '@angular/core';
import { Router } from '@angular/router';
import { GameService } from '../../services/game.service';
import { LobbyService } from '../../services/lobby.service';

@Component({
  selector: 'app-game-over',
  standalone: true,
  templateUrl: './game-over.html',
  styleUrl: './game-over.scss',
})
export class GameOverComponent {
  readonly gameService = inject(GameService);
  readonly lobby = inject(LobbyService);
  private router = inject(Router);

  constructor() {
    effect(() => {
      if (this.gameService.returnedToLobby()) {
        this.gameService.reset();
        this.router.navigate(['/lobby']);
      }
    });
  }

  returnToLobby(): void {
    this.gameService.returnToLobby();
  }

  get reasonText(): string {
    const reason = this.gameService.gameOver()?.reason;
    switch (reason) {
      case 'ALL_DEAD':
        return 'Alle Spieler wurden abgeschossen';
      case 'TIME_UP':
        return 'Zeit abgelaufen';
      case 'SCORE_REACHED':
        return 'Punktelimit erreicht';
      case 'ALL_LEFT':
        return 'Alle Spieler haben die Verbindung verloren';
      default:
        return 'Spiel beendet';
    }
  }
}
