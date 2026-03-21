import {
  Component,
  signal,
  computed,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef,
  AfterViewInit,
  NgZone,
} from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { WebSocketService } from '../services/websocket.service';
import { Renderer, PlayerRenderState, ProjectileRenderState } from './renderer';

@Component({
  selector: 'app-game',
  standalone: true,
  templateUrl: './game.html',
  styleUrl: './game.scss',
})
export class Game implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('gameCanvas') canvasRef!: ElementRef<HTMLCanvasElement>;

  private sub?: Subscription;
  private renderer?: Renderer;
  private animFrameId = 0;
  private inputSeq = 0;
  private currentInput = { up: false, down: false, left: false, right: false, shoot: false };
  private playerColors = new Map<string, string>();

  players = signal<PlayerRenderState[]>([]);
  projectiles = signal<ProjectileRenderState[]>([]);
  tick = signal(0);

  myPlayer = computed(() => this.players().find(p => p.id === this.ws.playerId()));
  score = computed(() => this.myPlayer()?.score ?? 0);
  hp = computed(() => this.myPlayer()?.hp ?? 0);
  alive = computed(() => this.myPlayer()?.alive ?? true);
  respawnIn = computed(() => Math.ceil((this.myPlayer()?.respawnIn ?? 0) / 30));

  scoreboard = computed(() =>
    [...this.players()].sort((a, b) => b.score - a.score)
  );

  constructor(
    public ws: WebSocketService,
    private router: Router,
    private zone: NgZone
  ) {}

  ngOnInit() {
    this.sub = this.ws.messages$.subscribe(msg => {
      switch (msg.type) {
        case 'GAME_STARTING':
          if (msg.data.players) {
            for (const p of msg.data.players) {
              this.playerColors.set(p.id, p.color);
            }
          }
          break;
        case 'GAME_STATE':
          this.tick.set(msg.data.tick);
          this.players.set(
            msg.data.players.map((p: any) => ({
              ...p,
              color: this.playerColors.get(p.id) ?? '#fff',
              name: this.playerColors.has(p.id) ? p.id : p.id,
            }))
          );
          this.projectiles.set(msg.data.projectiles ?? []);
          break;
        case 'GAME_OVER':
          this.router.navigate(['/game-over'], {
            state: { data: msg.data },
          });
          break;
        case 'LOBBY_STATE':
          this.router.navigate(['/']);
          break;
      }
    });

    window.addEventListener('keydown', this.onKeyDown);
    window.addEventListener('keyup', this.onKeyUp);
  }

  ngAfterViewInit() {
    this.renderer = new Renderer(this.canvasRef.nativeElement);
    this.zone.runOutsideAngular(() => this.gameLoop());
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
    cancelAnimationFrame(this.animFrameId);
    window.removeEventListener('keydown', this.onKeyDown);
    window.removeEventListener('keyup', this.onKeyUp);
  }

  private gameLoop = () => {
    this.renderer?.render(this.players(), this.projectiles(), this.ws.playerId());
    this.animFrameId = requestAnimationFrame(this.gameLoop);
  };

  private onKeyDown = (e: KeyboardEvent) => {
    if (this.updateInput(e.key, true)) {
      this.sendInput();
    }
  };

  private onKeyUp = (e: KeyboardEvent) => {
    if (this.updateInput(e.key, false)) {
      this.sendInput();
    }
  };

  private updateInput(key: string, pressed: boolean): boolean {
    const prev = { ...this.currentInput };

    switch (key.toLowerCase()) {
      case 'w': case 'arrowup':    this.currentInput.up = pressed; break;
      case 's': case 'arrowdown':  this.currentInput.down = pressed; break;
      case 'a': case 'arrowleft':  this.currentInput.left = pressed; break;
      case 'd': case 'arrowright': this.currentInput.right = pressed; break;
      case ' ':                    this.currentInput.shoot = pressed; break;
      default: return false;
    }

    return (
      prev.up !== this.currentInput.up ||
      prev.down !== this.currentInput.down ||
      prev.left !== this.currentInput.left ||
      prev.right !== this.currentInput.right ||
      prev.shoot !== this.currentInput.shoot
    );
  }

  private sendInput() {
    this.inputSeq++;
    this.ws.send('PLAYER_INPUT', { ...this.currentInput, seq: this.inputSeq });
  }
}
