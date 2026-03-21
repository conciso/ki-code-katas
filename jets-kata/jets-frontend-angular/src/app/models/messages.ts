export type GameMode = 'COOP' | 'FFA';

export type ErrorCode =
  | 'LOBBY_FULL'
  | 'LOBBY_NOT_FOUND'
  | 'GAME_IN_PROGRESS'
  | 'NOT_HOST'
  | 'INVALID_MESSAGE';

export type PowerUpType = 'RAPID_FIRE' | 'SHIELD' | 'SPEED_BOOST' | 'HEALTH_PACK';

export type EnemyType = 'SCOUT' | 'FIGHTER' | 'BOMBER' | 'BOSS';

export type GameEventType =
  | 'EXPLOSION'
  | 'PLAYER_HIT'
  | 'PLAYER_KILLED'
  | 'PLAYER_RESPAWN'
  | 'POWERUP_SPAWN'
  | 'POWERUP_PICKUP'
  | 'WAVE_START'
  | 'BOSS_SPAWN';

export type GameOverReason = 'ALL_DEAD' | 'TIME_UP' | 'SCORE_REACHED' | 'ALL_LEFT';

export type MessageType =
  | 'CONNECTED'
  | 'DISCONNECTED'
  | 'PING'
  | 'PONG'
  | 'ERROR'
  | 'CREATE_LOBBY'
  | 'LOBBY_CREATED'
  | 'JOIN_LOBBY'
  | 'LOBBY_STATE'
  | 'SET_GAME_MODE'
  | 'PLAYER_READY'
  | 'START_GAME'
  | 'LEAVE_LOBBY'
  | 'GAME_STARTING'
  | 'PLAYER_INPUT'
  | 'GAME_STATE'
  | 'GAME_EVENT'
  | 'WAVE_COMPLETE'
  | 'GAME_OVER'
  | 'RETURN_TO_LOBBY'
  | 'GAME_STATE_DELTA'
  | 'REQUEST_FULL_STATE';

export interface WsMessage<T = unknown> {
  type: MessageType;
  data: T;
}

export interface ConnectedData {
  playerId: string;
  serverTickRate: number;
}

export interface DisconnectedData {
  playerId: string;
  playerName: string;
}

export interface ErrorData {
  code: ErrorCode;
  message: string;
}

export interface LobbyCreatedData {
  lobbyCode: string;
  hostId: string;
}

export interface LobbyPlayer {
  id: string;
  name: string;
  ready: boolean;
  color: string;
}

export interface LobbyStateData {
  lobbyCode: string;
  hostId: string;
  gameMode: GameMode;
  players: LobbyPlayer[];
}

export interface GameStartingPlayer {
  id: string;
  name: string;
  color: string;
  spawnX: number;
  spawnY: number;
}

export interface GameStartingData {
  countdown: number;
  gameMode: GameMode;
  fieldWidth: number;
  fieldHeight: number;
  players: GameStartingPlayer[];
}

export interface PlayerInputData {
  up: boolean;
  down: boolean;
  left: boolean;
  right: boolean;
  shoot: boolean;
  seq: number;
}

export interface GamePlayer {
  id: string;
  x: number;
  y: number;
  hp: number;
  score: number;
  alive: boolean;
  respawnIn: number;
  invincible: boolean;
  activePowerUp: PowerUpType | null;
  lastProcessedInput: number;
}

export interface Projectile {
  id: string;
  x: number;
  y: number;
  vx: number;
  vy: number;
  owner: string;
}

export interface Enemy {
  id: string;
  type: EnemyType;
  x: number;
  y: number;
  hp: number;
}

export interface PowerUp {
  id: string;
  type: PowerUpType;
  x: number;
  y: number;
}

export interface GameStateData {
  tick: number;
  players: GamePlayer[];
  projectiles: Projectile[];
  enemies: Enemy[];
  powerUps: PowerUp[];
  wave: number;
  enemiesRemaining: number;
}

export interface GameEventData {
  event: GameEventType;
  x: number;
  y: number;
  details: Record<string, unknown>;
}

export interface WaveCompleteData {
  wave: number;
  nextWaveIn: number;
  scores: Record<string, number>;
}

export interface FinalScore {
  playerId: string;
  name: string;
  score: number;
  kills: number;
}

export interface GameOverData {
  reason: GameOverReason;
  finalScores: FinalScore[];
  totalScore: number;
  wavesCompleted: number;
}
