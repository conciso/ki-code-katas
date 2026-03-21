export interface PlayerRenderState {
  id: string;
  x: number;
  y: number;
  angle?: number;
  hp: number;
  score: number;
  alive: boolean;
  respawnIn: number;
  invincible: boolean;
  color: string;
  name: string;
}

export interface ProjectileRenderState {
  x: number;
  y: number;
  owner: string;
}

export class Renderer {
  private ctx: CanvasRenderingContext2D;
  private fieldWidth = 1920;
  private fieldHeight = 1080;
  private stars: { x: number; y: number; size: number; brightness: number }[] = [];
  private frameCount = 0;

  constructor(private canvas: HTMLCanvasElement) {
    this.ctx = canvas.getContext('2d')!;
    this.generateStars();
  }

  private generateStars() {
    for (let i = 0; i < 100; i++) {
      this.stars.push({
        x: Math.random() * this.fieldWidth,
        y: Math.random() * this.fieldHeight,
        size: Math.random() * 2 + 0.5,
        brightness: Math.random() * 0.5 + 0.3,
      });
    }
  }

  setFieldSize(w: number, h: number) {
    this.fieldWidth = w;
    this.fieldHeight = h;
  }

  render(
    players: PlayerRenderState[],
    projectiles: ProjectileRenderState[],
    myId: string | null
  ) {
    this.frameCount++;
    this.resizeCanvas();

    const scaleX = this.canvas.width / this.fieldWidth;
    const scaleY = this.canvas.height / this.fieldHeight;
    const scale = Math.min(scaleX, scaleY);
    const offsetX = (this.canvas.width - this.fieldWidth * scale) / 2;
    const offsetY = (this.canvas.height - this.fieldHeight * scale) / 2;

    this.ctx.save();
    this.ctx.translate(offsetX, offsetY);
    this.ctx.scale(scale, scale);

    this.drawBackground();
    this.drawProjectiles(projectiles, players);
    this.drawPlayers(players, myId);

    this.ctx.restore();
  }

  private resizeCanvas() {
    if (
      this.canvas.width !== this.canvas.clientWidth ||
      this.canvas.height !== this.canvas.clientHeight
    ) {
      this.canvas.width = this.canvas.clientWidth;
      this.canvas.height = this.canvas.clientHeight;
    }
  }

  private drawBackground() {
    this.ctx.fillStyle = '#0a0a1a';
    this.ctx.fillRect(0, 0, this.fieldWidth, this.fieldHeight);

    // Grid
    this.ctx.strokeStyle = 'rgba(255, 255, 255, 0.03)';
    this.ctx.lineWidth = 1;
    for (let x = 0; x < this.fieldWidth; x += 80) {
      this.ctx.beginPath();
      this.ctx.moveTo(x, 0);
      this.ctx.lineTo(x, this.fieldHeight);
      this.ctx.stroke();
    }
    for (let y = 0; y < this.fieldHeight; y += 80) {
      this.ctx.beginPath();
      this.ctx.moveTo(0, y);
      this.ctx.lineTo(this.fieldWidth, y);
      this.ctx.stroke();
    }

    // Stars
    for (const star of this.stars) {
      const flicker = star.brightness + Math.sin(this.frameCount * 0.02 + star.x) * 0.15;
      this.ctx.fillStyle = `rgba(255, 255, 255, ${flicker})`;
      this.ctx.beginPath();
      this.ctx.arc(star.x, star.y, star.size, 0, Math.PI * 2);
      this.ctx.fill();
    }
  }

  private drawPlayers(players: PlayerRenderState[], myId: string | null) {
    for (const p of players) {
      if (!p.alive) {
        if (p.respawnIn > 0) {
          this.drawRespawnTimer(p);
        }
        continue;
      }

      // Invincibility blink
      if (p.invincible && this.frameCount % 10 < 5) continue;

      this.drawJet(p, p.id === myId);
      this.drawNameTag(p);
    }
  }

  private drawJet(p: PlayerRenderState, isMe: boolean) {
    const size = 18;
    const angle = p.angle ?? 0;

    this.ctx.save();
    this.ctx.translate(p.x, p.y);
    this.ctx.rotate(angle);

    // Glow
    if (isMe) {
      this.ctx.shadowColor = p.color;
      this.ctx.shadowBlur = 15;
    }

    // Jet body (triangle)
    this.ctx.fillStyle = p.color;
    this.ctx.beginPath();
    this.ctx.moveTo(size, 0);
    this.ctx.lineTo(-size * 0.7, -size * 0.6);
    this.ctx.lineTo(-size * 0.4, 0);
    this.ctx.lineTo(-size * 0.7, size * 0.6);
    this.ctx.closePath();
    this.ctx.fill();

    // Outline
    this.ctx.strokeStyle = 'rgba(255, 255, 255, 0.5)';
    this.ctx.lineWidth = 1;
    this.ctx.stroke();

    this.ctx.restore();
  }

  private drawNameTag(p: PlayerRenderState) {
    this.ctx.fillStyle = 'rgba(255, 255, 255, 0.6)';
    this.ctx.font = '10px "JetBrains Mono", monospace';
    this.ctx.textAlign = 'center';
    this.ctx.fillText(p.name, p.x, p.y - 25);

    // HP dots
    const dotSize = 3;
    const startX = p.x - ((p.hp - 1) * (dotSize * 3)) / 2;
    for (let i = 0; i < p.hp; i++) {
      this.ctx.fillStyle = p.color;
      this.ctx.beginPath();
      this.ctx.arc(startX + i * dotSize * 3, p.y - 35, dotSize, 0, Math.PI * 2);
      this.ctx.fill();
    }
  }

  private drawRespawnTimer(p: PlayerRenderState) {
    const seconds = Math.ceil(p.respawnIn / 30);
    this.ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
    this.ctx.font = '16px "Press Start 2P", monospace';
    this.ctx.textAlign = 'center';
    this.ctx.fillText(`${seconds}`, p.x, p.y + 5);
  }

  private drawProjectiles(projectiles: ProjectileRenderState[], players: PlayerRenderState[]) {
    for (const proj of projectiles) {
      const owner = players.find(p => p.id === proj.owner);
      const color = owner?.color ?? '#fff';

      this.ctx.shadowColor = color;
      this.ctx.shadowBlur = 8;
      this.ctx.fillStyle = color;
      this.ctx.beginPath();
      this.ctx.arc(proj.x, proj.y, 3, 0, Math.PI * 2);
      this.ctx.fill();
      this.ctx.shadowBlur = 0;
    }
  }
}
