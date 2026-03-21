import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef,
  inject,
  effect,
} from '@angular/core';
import { Router } from '@angular/router';
import { GameService } from '../../services/game.service';
import { LobbyService } from '../../services/lobby.service';
import { Enemy, GamePlayer, PowerUp, Projectile } from '../../models/messages';

interface PendingInput {
  up: boolean;
  down: boolean;
  left: boolean;
  right: boolean;
  shoot: boolean;
  seq: number;
}

@Component({
  selector: 'app-game',
  standalone: true,
  templateUrl: './game.html',
  styleUrl: './game.scss',
})
export class GameComponent implements OnInit, OnDestroy {
  @ViewChild('gameCanvas', { static: true }) canvasRef!: ElementRef<HTMLCanvasElement>;

  private gameService = inject(GameService);
  private lobbyService = inject(LobbyService);
  private router = inject(Router);

  private ctx!: CanvasRenderingContext2D;
  private animFrameId = 0;
  private keys = new Set<string>();
  private lastInput = { up: false, down: false, left: false, right: false, shoot: false };
  private pendingInputs: PendingInput[] = [];

  private fieldWidth = 1920;
  private fieldHeight = 1080;
  private playerColors = new Map<string, string>();
  private playerNames = new Map<string, string>();

  private readonly PLAYER_SPEED = 5;
  private readonly PLAYER_SIZE = 20;

  private keyDownHandler = (e: KeyboardEvent) => this.handleKeyDown(e);
  private keyUpHandler = (e: KeyboardEvent) => this.handleKeyUp(e);
  private resizeHandler = () => this.resizeCanvas();

  constructor() {
    effect(() => {
      if (this.gameService.gameOver()) {
        this.router.navigate(['/game-over']);
      }
    });
  }

  ngOnInit(): void {
    const canvas = this.canvasRef.nativeElement;
    this.ctx = canvas.getContext('2d')!;

    this.resizeCanvas();
    window.addEventListener('resize', this.resizeHandler);
    window.addEventListener('keydown', this.keyDownHandler);
    window.addEventListener('keyup', this.keyUpHandler);

    const lobby = this.lobbyService.lobbyState();
    if (lobby) {
      for (const p of lobby.players) {
        this.playerColors.set(p.id, p.color);
        this.playerNames.set(p.id, p.name);
      }
    }

    const starting = this.lobbyService.gameStarting();
    if (starting) {
      this.fieldWidth = starting.fieldWidth;
      this.fieldHeight = starting.fieldHeight;
      for (const p of starting.players) {
        this.playerColors.set(p.id, p.color);
        this.playerNames.set(p.id, p.name);
      }
    }

    this.startLoop();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animFrameId);
    window.removeEventListener('resize', this.resizeHandler);
    window.removeEventListener('keydown', this.keyDownHandler);
    window.removeEventListener('keyup', this.keyUpHandler);
  }

  private resizeCanvas(): void {
    const canvas = this.canvasRef.nativeElement;
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
  }

  private handleKeyDown(e: KeyboardEvent): void {
    if (['Space', 'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(e.code)) {
      e.preventDefault();
    }
    this.keys.add(e.code);
    this.updateInput();
  }

  private handleKeyUp(e: KeyboardEvent): void {
    this.keys.delete(e.code);
    this.updateInput();
  }

  private updateInput(): void {
    const up = this.keys.has('KeyW') || this.keys.has('ArrowUp');
    const down = this.keys.has('KeyS') || this.keys.has('ArrowDown');
    const left = this.keys.has('KeyA') || this.keys.has('ArrowLeft');
    const right = this.keys.has('KeyD') || this.keys.has('ArrowRight');
    const shoot = this.keys.has('Space');

    if (
      up !== this.lastInput.up ||
      down !== this.lastInput.down ||
      left !== this.lastInput.left ||
      right !== this.lastInput.right ||
      shoot !== this.lastInput.shoot
    ) {
      this.lastInput = { up, down, left, right, shoot };
      const seq = this.gameService.sendInput({ up, down, left, right, shoot });
      this.pendingInputs.push({ up, down, left, right, shoot, seq });
      if (this.pendingInputs.length > 120) {
        this.pendingInputs.shift();
      }
    }
  }

  private startLoop(): void {
    const loop = () => {
      this.render();
      this.animFrameId = requestAnimationFrame(loop);
    };
    this.animFrameId = requestAnimationFrame(loop);
  }

  private render(): void {
    const canvas = this.canvasRef.nativeElement;
    const ctx = this.ctx;
    const state = this.gameService.gameState();

    ctx.fillStyle = '#060c14';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    if (!state) return;

    const scaleX = canvas.width / this.fieldWidth;
    const scaleY = canvas.height / this.fieldHeight;
    const scale = Math.min(scaleX, scaleY);
    const offsetX = (canvas.width - this.fieldWidth * scale) / 2;
    const offsetY = (canvas.height - this.fieldHeight * scale) / 2;

    ctx.save();
    ctx.translate(offsetX, offsetY);
    ctx.scale(scale, scale);

    // Spielfeld
    ctx.strokeStyle = '#1a3a5a';
    ctx.lineWidth = 4 / scale;
    ctx.strokeRect(0, 0, this.fieldWidth, this.fieldHeight);

    // Grid
    ctx.strokeStyle = '#0b1825';
    ctx.lineWidth = 1 / scale;
    for (let x = 192; x < this.fieldWidth; x += 192) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, this.fieldHeight);
      ctx.stroke();
    }
    for (let y = 108; y < this.fieldHeight; y += 108) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(this.fieldWidth, y);
      ctx.stroke();
    }

    for (const pu of state.powerUps) {
      this.drawPowerUp(ctx, pu, state.tick);
    }

    for (const proj of state.projectiles) {
      this.drawProjectile(ctx, proj);
    }

    for (const enemy of state.enemies) {
      this.drawEnemy(ctx, enemy);
    }

    const myId = this.lobbyService.playerId();
    for (const player of state.players) {
      if (!player.alive) continue;
      const color = this.playerColors.get(player.id) ?? '#ffffff';
      const isMe = player.id === myId;

      let x = player.x;
      let y = player.y;

      if (isMe) {
        const pending = this.pendingInputs.filter((i) => i.seq > player.lastProcessedInput);
        const speed = this.PLAYER_SPEED;
        for (const input of pending) {
          if (input.up) y -= speed;
          if (input.down) y += speed;
          if (input.left) x -= speed;
          if (input.right) x += speed;
          x = Math.max(0, Math.min(this.fieldWidth, x));
          y = Math.max(0, Math.min(this.fieldHeight, y));
        }
      }

      this.drawPlayer(ctx, x, y, color, player, isMe);
    }

    const now = Date.now();
    for (const fx of this.gameService.effects()) {
      const age = (now - fx.createdAt) / 500;
      if (age < 1) {
        this.drawExplosion(ctx, fx.x, fx.y, age);
      }
    }

    ctx.restore();

    this.drawHUD(ctx, state, canvas.width, canvas.height, myId);
  }

  private drawPlayer(
    ctx: CanvasRenderingContext2D,
    x: number,
    y: number,
    color: string,
    player: GamePlayer,
    isMe: boolean,
  ): void {
    ctx.save();
    ctx.translate(x, y);

    if (player.invincible && Math.floor(Date.now() / 120) % 2 === 0) {
      ctx.globalAlpha = 0.35;
    }

    const s = this.PLAYER_SIZE;

    // Triebwerks-Flamme
    ctx.fillStyle = `rgba(255,140,0,${0.4 + 0.3 * Math.random()})`;
    ctx.beginPath();
    ctx.moveTo(-s * 0.25, s * 0.2);
    ctx.lineTo(s * 0.25, s * 0.2);
    ctx.lineTo(0, s * 0.6 + Math.random() * 8);
    ctx.closePath();
    ctx.fill();

    // Rumpf
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.moveTo(0, -s);
    ctx.lineTo(-s * 0.5, s * 0.5);
    ctx.lineTo(0, s * 0.15);
    ctx.lineTo(s * 0.5, s * 0.5);
    ctx.closePath();
    ctx.fill();

    // Flügel
    ctx.globalAlpha *= 0.75;
    ctx.beginPath();
    ctx.moveTo(-s * 0.5, 0);
    ctx.lineTo(-s * 1.1, s * 0.45);
    ctx.lineTo(-s * 0.1, s * 0.1);
    ctx.closePath();
    ctx.fill();
    ctx.beginPath();
    ctx.moveTo(s * 0.5, 0);
    ctx.lineTo(s * 1.1, s * 0.45);
    ctx.lineTo(s * 0.1, s * 0.1);
    ctx.closePath();
    ctx.fill();
    ctx.globalAlpha = 1;

    if (isMe) {
      ctx.strokeStyle = 'rgba(255,255,255,0.8)';
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.moveTo(0, -s);
      ctx.lineTo(-s * 0.5, s * 0.5);
      ctx.lineTo(0, s * 0.15);
      ctx.lineTo(s * 0.5, s * 0.5);
      ctx.closePath();
      ctx.stroke();
    }

    if (player.activePowerUp === 'SHIELD') {
      ctx.strokeStyle = '#44aaff';
      ctx.lineWidth = 2.5;
      ctx.globalAlpha = 0.5 + 0.5 * Math.sin(Date.now() / 180);
      ctx.beginPath();
      ctx.arc(0, 0, s * 1.6, 0, Math.PI * 2);
      ctx.stroke();
      ctx.globalAlpha = 1;
    }

    // HP-Balken
    const hpW = 44;
    const hpH = 4;
    ctx.fillStyle = '#1a2a1a';
    ctx.fillRect(-hpW / 2, -s - 14, hpW, hpH);
    ctx.fillStyle = player.hp > 1 ? '#44cc66' : '#ff4444';
    ctx.fillRect(-hpW / 2, -s - 14, hpW * (player.hp / 3), hpH);

    const name = this.playerNames.get(player.id) ?? '?';
    ctx.fillStyle = 'rgba(220,235,255,0.85)';
    ctx.font = `${9 / (1 / this.PLAYER_SIZE)}px monospace`;
    ctx.font = '9px monospace';
    ctx.textAlign = 'center';
    ctx.fillText(name, 0, -s - 18);

    ctx.restore();
  }

  private drawProjectile(ctx: CanvasRenderingContext2D, proj: Projectile): void {
    const color = this.playerColors.get(proj.owner) ?? '#ffff88';
    ctx.save();
    ctx.fillStyle = color;
    ctx.shadowColor = color;
    ctx.shadowBlur = 8;
    ctx.beginPath();
    ctx.arc(proj.x, proj.y, 3.5, 0, Math.PI * 2);
    ctx.fill();

    // Spur
    ctx.strokeStyle = color;
    ctx.globalAlpha = 0.35;
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(proj.x, proj.y);
    ctx.lineTo(proj.x - proj.vx * 3, proj.y - proj.vy * 3);
    ctx.stroke();

    ctx.restore();
  }

  private drawEnemy(ctx: CanvasRenderingContext2D, enemy: Enemy): void {
    ctx.save();
    ctx.translate(enemy.x, enemy.y);

    const colors: Record<string, string> = {
      SCOUT: '#ffdd44',
      FIGHTER: '#ff8800',
      BOMBER: '#ff3333',
      BOSS: '#cc44ff',
    };
    const maxHp: Record<string, number> = { SCOUT: 1, FIGHTER: 2, BOMBER: 4, BOSS: 20 };
    const sizes: Record<string, number> = { SCOUT: 14, FIGHTER: 18, BOMBER: 24, BOSS: 40 };

    const color = colors[enemy.type] ?? '#ff4444';
    const s = sizes[enemy.type] ?? 16;

    // Gegner-Jet zeigt nach UNTEN
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.moveTo(0, s);
    ctx.lineTo(-s * 0.5, -s * 0.5);
    ctx.lineTo(0, -s * 0.15);
    ctx.lineTo(s * 0.5, -s * 0.5);
    ctx.closePath();
    ctx.fill();

    // Flügel
    ctx.globalAlpha = 0.75;
    ctx.beginPath();
    ctx.moveTo(-s * 0.5, 0);
    ctx.lineTo(-s * 1.1, -s * 0.45);
    ctx.lineTo(-s * 0.1, -s * 0.1);
    ctx.closePath();
    ctx.fill();
    ctx.beginPath();
    ctx.moveTo(s * 0.5, 0);
    ctx.lineTo(s * 1.1, -s * 0.45);
    ctx.lineTo(s * 0.1, -s * 0.1);
    ctx.closePath();
    ctx.fill();
    ctx.globalAlpha = 1;

    if (enemy.hp > 1) {
      const max = maxHp[enemy.type] ?? 1;
      const bw = s * 2;
      ctx.fillStyle = '#1a1a1a';
      ctx.fillRect(-bw / 2, s + 6, bw, 4);
      ctx.fillStyle = color;
      ctx.fillRect(-bw / 2, s + 6, bw * (enemy.hp / max), 4);
    }

    ctx.restore();
  }

  private drawPowerUp(ctx: CanvasRenderingContext2D, pu: PowerUp, tick: number): void {
    const colors: Record<string, string> = {
      RAPID_FIRE: '#ff8800',
      SHIELD: '#44aaff',
      SPEED_BOOST: '#44ff88',
      HEALTH_PACK: '#ff4477',
    };
    const labels: Record<string, string> = {
      RAPID_FIRE: 'RF',
      SHIELD: 'SH',
      SPEED_BOOST: 'SP',
      HEALTH_PACK: '+',
    };

    const color = colors[pu.type] ?? '#fff';
    const label = labels[pu.type] ?? '?';
    const pulse = 0.65 + 0.35 * Math.sin(tick / 4);

    ctx.save();
    ctx.translate(pu.x, pu.y);
    ctx.globalAlpha = pulse;

    ctx.strokeStyle = color;
    ctx.lineWidth = 2;
    ctx.shadowColor = color;
    ctx.shadowBlur = 10;
    ctx.beginPath();
    ctx.arc(0, 0, 14, 0, Math.PI * 2);
    ctx.stroke();

    ctx.shadowBlur = 0;
    ctx.fillStyle = color;
    ctx.font = 'bold 11px monospace';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(label, 0, 0);

    ctx.restore();
  }

  private drawExplosion(
    ctx: CanvasRenderingContext2D,
    x: number,
    y: number,
    progress: number,
  ): void {
    ctx.save();
    ctx.globalAlpha = 1 - progress;

    const r1 = 50 * progress;
    const r2 = 25 * progress;

    ctx.strokeStyle = '#ff8800';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.arc(x, y, r1, 0, Math.PI * 2);
    ctx.stroke();

    ctx.strokeStyle = '#ffdd44';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(x, y, r2, 0, Math.PI * 2);
    ctx.stroke();

    ctx.fillStyle = `rgba(255,200,50,${0.3 * (1 - progress)})`;
    ctx.beginPath();
    ctx.arc(x, y, r2 * 0.6, 0, Math.PI * 2);
    ctx.fill();

    ctx.restore();
  }

  private drawHUD(
    ctx: CanvasRenderingContext2D,
    state: { tick: number; players: GamePlayer[]; wave: number; enemiesRemaining: number },
    cw: number,
    ch: number,
    myId: string | null,
  ): void {
    // --- Wave-Info oben links ---
    ctx.fillStyle = 'rgba(0,0,0,0.55)';
    ctx.fillRect(12, 12, 190, 52);
    ctx.fillStyle = '#66aadd';
    ctx.font = 'bold 15px monospace';
    ctx.textAlign = 'left';
    ctx.fillText(`Wave ${state.wave}`, 22, 33);
    ctx.fillStyle = '#556677';
    ctx.font = '12px monospace';
    ctx.fillText(`Gegner: ${state.enemiesRemaining}`, 22, 52);

    // --- Scores oben rechts ---
    let sy = 14;
    for (const player of state.players) {
      const name = this.playerNames.get(player.id) ?? player.id.slice(0, 6);
      const color = this.playerColors.get(player.id) ?? '#fff';
      const isMe = player.id === myId;

      ctx.fillStyle = 'rgba(0,0,0,0.55)';
      ctx.fillRect(cw - 215, sy - 2, 203, 24);
      ctx.fillStyle = color;
      ctx.font = isMe ? 'bold 13px monospace' : '12px monospace';
      ctx.textAlign = 'right';
      ctx.fillText(`${name}: ${player.score}`, cw - 18, sy + 15);

      sy += 30;
    }

    // --- Mein HP + Power-Up unten links ---
    const myPlayer = state.players.find((p) => p.id === myId);
    if (myPlayer) {
      ctx.fillStyle = 'rgba(0,0,0,0.55)';
      ctx.fillRect(12, ch - 62, 210, 50);

      ctx.font = '22px monospace';
      ctx.textAlign = 'left';
      const hearts = '♥'.repeat(Math.max(0, myPlayer.hp)) + '♡'.repeat(Math.max(0, 3 - myPlayer.hp));
      ctx.fillStyle = myPlayer.hp > 1 ? '#ff4477' : '#ff2222';
      ctx.fillText(hearts, 20, ch - 33);

      if (myPlayer.activePowerUp) {
        const puColors: Record<string, string> = {
          RAPID_FIRE: '#ff8800',
          SHIELD: '#44aaff',
          SPEED_BOOST: '#44ff88',
          HEALTH_PACK: '#ff4477',
        };
        ctx.fillStyle = puColors[myPlayer.activePowerUp] ?? '#fff';
        ctx.font = '11px monospace';
        ctx.fillText(`>> ${myPlayer.activePowerUp.replace('_', ' ')}`, 20, ch - 17);
      }

      if (!myPlayer.alive) {
        ctx.fillStyle = 'rgba(0,0,0,0.72)';
        ctx.fillRect(cw / 2 - 160, ch / 2 - 45, 320, 90);
        ctx.strokeStyle = '#ff3333';
        ctx.lineWidth = 1;
        ctx.strokeRect(cw / 2 - 160, ch / 2 - 45, 320, 90);

        ctx.fillStyle = '#ff4444';
        ctx.font = 'bold 26px monospace';
        ctx.textAlign = 'center';
        ctx.fillText('ABGESCHOSSEN', cw / 2, ch / 2);

        if (myPlayer.respawnIn > 0) {
          ctx.fillStyle = '#aaa';
          ctx.font = '14px monospace';
          ctx.fillText(`Respawn in ${(myPlayer.respawnIn / 30).toFixed(1)}s`, cw / 2, ch / 2 + 28);
        }
      }
    }

    // --- Wave Complete Banner ---
    const wc = this.gameService.waveComplete();
    if (wc) {
      ctx.fillStyle = 'rgba(0,0,0,0.65)';
      ctx.fillRect(cw / 2 - 220, ch / 2 - 55, 440, 110);
      ctx.strokeStyle = '#44cc88';
      ctx.lineWidth = 1;
      ctx.strokeRect(cw / 2 - 220, ch / 2 - 55, 440, 110);

      ctx.fillStyle = '#44ff99';
      ctx.font = 'bold 28px monospace';
      ctx.textAlign = 'center';
      ctx.fillText(`WELLE ${wc.wave} ABGESCHLOSSEN`, cw / 2, ch / 2 - 10);
      ctx.fillStyle = '#667788';
      ctx.font = '15px monospace';
      ctx.fillText(`Nächste Welle in ${wc.nextWaveIn}s`, cw / 2, ch / 2 + 25);
    }
  }
}
