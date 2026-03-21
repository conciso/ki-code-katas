export interface Player {
  id: string
  name: string
  ready: boolean
  color: string
}

export interface Lobby {
  lobbyCode: string
  hostId: string
  gameMode: 'COOP' | 'FFA'
  players: Player[]
}

export interface GamePlayer {
  id: string
  x: number
  y: number
  hp: number
  score: number
  alive: boolean
  respawnIn: number
  invincible: boolean
  activePowerUp: string | null
  lastProcessedInput: number
}

export interface Projectile {
  id: string
  x: number
  y: number
  vx: number
  vy: number
  owner: string
}

export interface Enemy {
  id: string
  type: string
  x: number
  y: number
  hp: number
}

export interface PowerUp {
  id: string
  type: string
  x: number
  y: number
}

export interface GameState {
  tick: number
  players: GamePlayer[]
  projectiles: Projectile[]
  enemies: Enemy[]
  powerUps: PowerUp[]
  wave: number
  enemiesRemaining: number
}

export interface GameStartingPlayer {
  id: string
  name: string
  color: string
  spawnX: number
  spawnY: number
}

export interface GameStarting {
  countdown: number
  gameMode: 'COOP' | 'FFA'
  fieldWidth: number
  fieldHeight: number
  players: GameStartingPlayer[]
}

export interface WaveComplete {
  wave: number
  nextWaveIn: number
  scores: Record<string, number>
}

export interface GameOverScore {
  playerId: string
  name: string
  score: number
  kills: number
}

export interface GameOver {
  reason: 'ALL_DEAD' | 'TIME_UP' | 'SCORE_REACHED' | 'ALL_LEFT'
  finalScores: GameOverScore[]
  totalScore: number
  wavesCompleted: number
}
