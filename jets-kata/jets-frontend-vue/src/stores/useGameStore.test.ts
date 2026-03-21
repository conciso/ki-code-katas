import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useGameStore } from './useGameStore'

class MockWebSocket {
  static instances: MockWebSocket[] = []
  url: string
  onopen: ((event: Event) => void) | null = null
  onmessage: ((event: MessageEvent) => void) | null = null
  onclose: ((event: CloseEvent) => void) | null = null
  onerror: ((event: Event) => void) | null = null
  readyState = WebSocket.CONNECTING

  constructor(url: string) {
    this.url = url
    MockWebSocket.instances.push(this)
  }

  send = vi.fn()
  close = vi.fn()

  simulateOpen() {
    this.readyState = WebSocket.OPEN
    this.onopen?.(new Event('open'))
  }

  simulateMessage(data: object) {
    this.onmessage?.(new MessageEvent('message', { data: JSON.stringify(data) }))
  }

  simulateClose() {
    this.readyState = WebSocket.CLOSED
    this.onclose?.(new Event('close') as CloseEvent)
  }
}

describe('useGameStore — WebSocket', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  function connectAndOpen(store: ReturnType<typeof useGameStore>) {
    store.connect('Alice')
    const ws = MockWebSocket.instances[0]!
    ws.simulateOpen()
    ws.simulateMessage({ type: 'CONNECTED', data: { playerId: 'p1a2b3c4', serverTickRate: 30 } })
    return ws
  }

  it('ist initial nicht verbunden', () => {
    const store = useGameStore()

    expect(store.isConnected).toBe(false)
  })

  it('öffnet eine WebSocket-Verbindung mit dem Spielernamen', () => {
    const store = useGameStore()

    store.connect('Alice')

    expect(MockWebSocket.instances).toHaveLength(1)
    expect(MockWebSocket.instances[0]?.url).toContain('playerName=Alice')
  })

  it('verwendet VITE_WS_HOST und VITE_WS_PORT aus der Umgebung', () => {
    vi.stubEnv('VITE_WS_HOST', 'game.example.com')
    vi.stubEnv('VITE_WS_PORT', '9090')
    const store = useGameStore()

    store.connect('Alice')

    expect(MockWebSocket.instances[0]?.url).toBe('ws://game.example.com:9090/ws/game?playerName=Alice')
  })

  it('ist nach CONNECTED-Nachricht verbunden und kennt die playerId', () => {
    const store = useGameStore()

    store.connect('Alice')
    const ws = MockWebSocket.instances[0]!
    ws.simulateOpen()
    ws.simulateMessage({ type: 'CONNECTED', data: { playerId: 'p1a2b3c4', serverTickRate: 30 } })

    expect(store.isConnected).toBe(true)
    expect(store.playerId).toBe('p1a2b3c4')
  })

  it('ist nach Verbindungsabbruch nicht mehr verbunden', () => {
    const store = useGameStore()

    store.connect('Alice')
    const ws = MockWebSocket.instances[0]!
    ws.simulateOpen()
    ws.simulateMessage({ type: 'CONNECTED', data: { playerId: 'p1a2b3c4', serverTickRate: 30 } })
    ws.simulateClose()

    expect(store.isConnected).toBe(false)
  })
})

describe('useGameStore — GAME_EVENT', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  function connectAndOpen(store: ReturnType<typeof useGameStore>) {
    store.connect('Alice')
    const ws = MockWebSocket.instances[0]!
    ws.simulateOpen()
    ws.simulateMessage({ type: 'CONNECTED', data: { playerId: 'p1a2b3c4', serverTickRate: 30 } })
    return ws
  }

  it('ist initial leer', () => {
    const store = useGameStore()
    expect(store.gameEvents).toEqual([])
  })

  it('EXPLOSION wird gespeichert', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({
      type: 'GAME_EVENT',
      data: { event: 'EXPLOSION', x: 900, y: 50, details: { destroyedType: 'SCOUT', destroyedBy: 'p1a2b3c4' } },
    })

    expect(store.gameEvents).toHaveLength(1)
    expect(store.gameEvents[0]).toMatchObject({
      event: 'EXPLOSION',
      x: 900,
      y: 50,
      details: { destroyedType: 'SCOUT', destroyedBy: 'p1a2b3c4' },
    })
  })

  it('PLAYER_HIT wird gespeichert', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({
      type: 'GAME_EVENT',
      data: { event: 'PLAYER_HIT', x: 450, y: 320, details: { playerId: 'p1a2b3c4', damage: 1, hitBy: 'p2d5e6f7' } },
    })

    expect(store.gameEvents[0]).toMatchObject({
      event: 'PLAYER_HIT',
      details: { playerId: 'p1a2b3c4', damage: 1, hitBy: 'p2d5e6f7' },
    })
  })

  it('PLAYER_KILLED wird gespeichert', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({
      type: 'GAME_EVENT',
      data: { event: 'PLAYER_KILLED', x: 450, y: 320, details: { playerId: 'p1a2b3c4', killedBy: 'p2d5e6f7' } },
    })

    expect(store.gameEvents[0]).toMatchObject({
      event: 'PLAYER_KILLED',
      details: { playerId: 'p1a2b3c4', killedBy: 'p2d5e6f7' },
    })
  })

  it('PLAYER_RESPAWN wird gespeichert', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({
      type: 'GAME_EVENT',
      data: { event: 'PLAYER_RESPAWN', x: 200, y: 540, details: { playerId: 'p1a2b3c4', x: 200, y: 540 } },
    })

    expect(store.gameEvents[0]).toMatchObject({
      event: 'PLAYER_RESPAWN',
      details: { playerId: 'p1a2b3c4', x: 200, y: 540 },
    })
  })

  it('POWERUP_SPAWN wird gespeichert', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({
      type: 'GAME_EVENT',
      data: { event: 'POWERUP_SPAWN', x: 700, y: 400, details: { powerUpId: 'pu001', type: 'SHIELD', x: 700, y: 400 } },
    })

    expect(store.gameEvents[0]).toMatchObject({
      event: 'POWERUP_SPAWN',
      details: { powerUpId: 'pu001', type: 'SHIELD' },
    })
  })

  it('POWERUP_PICKUP wird gespeichert', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({
      type: 'GAME_EVENT',
      data: { event: 'POWERUP_PICKUP', x: 700, y: 400, details: { playerId: 'p1a2b3c4', type: 'RAPID_FIRE' } },
    })

    expect(store.gameEvents[0]).toMatchObject({
      event: 'POWERUP_PICKUP',
      details: { playerId: 'p1a2b3c4', type: 'RAPID_FIRE' },
    })
  })

  it('WAVE_START wird gespeichert', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({
      type: 'GAME_EVENT',
      data: { event: 'WAVE_START', x: 0, y: 0, details: { wave: 4, enemyCount: 12 } },
    })

    expect(store.gameEvents[0]).toMatchObject({
      event: 'WAVE_START',
      details: { wave: 4, enemyCount: 12 },
    })
  })

  it('BOSS_SPAWN wird gespeichert', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({
      type: 'GAME_EVENT',
      data: { event: 'BOSS_SPAWN', x: 960, y: 100, details: { enemyId: 'e099', x: 960, y: 100 } },
    })

    expect(store.gameEvents[0]).toMatchObject({
      event: 'BOSS_SPAWN',
      details: { enemyId: 'e099', x: 960, y: 100 },
    })
  })

  it('mehrere Events werden akkumuliert', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({
      type: 'GAME_EVENT',
      data: { event: 'WAVE_START', x: 0, y: 0, details: { wave: 1, enemyCount: 5 } },
    })
    ws.simulateMessage({
      type: 'GAME_EVENT',
      data: { event: 'EXPLOSION', x: 200, y: 100, details: { destroyedType: 'SCOUT', destroyedBy: 'p1a2b3c4' } },
    })

    expect(store.gameEvents).toHaveLength(2)
    expect(store.gameEvents[0]?.event).toBe('WAVE_START')
    expect(store.gameEvents[1]?.event).toBe('EXPLOSION')
  })
})

describe('useGameStore — ERROR', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  function connectAndOpen(store: ReturnType<typeof useGameStore>) {
    store.connect('Alice')
    const ws = MockWebSocket.instances[0]!
    ws.simulateOpen()
    ws.simulateMessage({ type: 'CONNECTED', data: { playerId: 'p1a2b3c4', serverTickRate: 30 } })
    return ws
  }

  it('lastError ist initial null', () => {
    const store = useGameStore()
    expect(store.lastError).toBeNull()
  })

  it('speichert den letzten Fehler', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({
      type: 'ERROR',
      data: { code: 'LOBBY_FULL', message: 'Die Lobby ist voll (max. 4 Spieler)' },
    })

    expect(store.lastError).toEqual({ code: 'LOBBY_FULL', message: 'Die Lobby ist voll (max. 4 Spieler)' })
  })

  it('überschreibt den vorherigen Fehler', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({ type: 'ERROR', data: { code: 'LOBBY_FULL', message: 'voll' } })
    ws.simulateMessage({ type: 'ERROR', data: { code: 'NOT_HOST', message: 'Nur der Host' } })

    expect(store.lastError?.code).toBe('NOT_HOST')
  })

  it.each([
    ['LOBBY_FULL', 'Lobby voll'],
    ['LOBBY_NOT_FOUND', 'Lobby nicht gefunden'],
    ['GAME_IN_PROGRESS', 'Spiel läuft bereits'],
    ['NOT_HOST', 'Nur der Host'],
    ['INVALID_MESSAGE', 'Ungültige Nachricht'],
  ])('speichert Fehlercode %s', (code, message) => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({ type: 'ERROR', data: { code, message } })

    expect(store.lastError?.code).toBe(code)
    expect(store.lastError?.message).toBe(message)
  })
})

describe('useGameStore — PING/PONG', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  function connectAndOpen(store: ReturnType<typeof useGameStore>) {
    store.connect('Alice')
    const ws = MockWebSocket.instances[0]!
    ws.simulateOpen()
    ws.simulateMessage({ type: 'CONNECTED', data: { playerId: 'p1a2b3c4', serverTickRate: 30 } })
    return ws
  }

  it('latency ist initial null', () => {
    const store = useGameStore()
    expect(store.latency).toBeNull()
  })

  it('ping() sendet eine PING-Nachricht mit Timestamp', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)
    vi.spyOn(Date, 'now').mockReturnValue(1710950400000)

    store.ping()

    expect(ws.send).toHaveBeenCalledWith(
      JSON.stringify({ type: 'PING', data: { timestamp: 1710950400000 } }),
    )
  })

  it('berechnet latency aus PONG-Antwort', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)
    vi.spyOn(Date, 'now').mockReturnValueOnce(1710950400000).mockReturnValueOnce(1710950400042)

    store.ping()
    ws.simulateMessage({ type: 'PONG', data: { timestamp: 1710950400000 } })

    expect(store.latency).toBe(42)
  })
})

describe('useGameStore — Lobby-Nachrichten', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  function connectAndOpen(store: ReturnType<typeof useGameStore>) {
    store.connect('Alice')
    const ws = MockWebSocket.instances[0]!
    ws.simulateOpen()
    ws.simulateMessage({ type: 'CONNECTED', data: { playerId: 'p1a2b3c4', serverTickRate: 30 } })
    return ws
  }

  it('lobby ist initial null', () => {
    const store = useGameStore()
    expect(store.lobby).toBeNull()
  })

  it('LOBBY_CREATED setzt lobbyCode und hostId', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({ type: 'LOBBY_CREATED', data: { lobbyCode: 'A3F9K2', hostId: 'p1a2b3c4' } })

    expect(store.lobby).toMatchObject({ lobbyCode: 'A3F9K2', hostId: 'p1a2b3c4' })
  })

  it('LOBBY_STATE aktualisiert den vollständigen Lobby-Zustand', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({
      type: 'LOBBY_STATE',
      data: {
        lobbyCode: 'A3F9K2',
        hostId: 'p1a2b3c4',
        gameMode: 'COOP',
        players: [
          { id: 'p1a2b3c4', name: 'Alice', ready: true, color: '#FF4444' },
          { id: 'p2d5e6f7', name: 'Bob', ready: false, color: '#4444FF' },
        ],
      },
    })

    expect(store.lobby).toMatchObject({
      lobbyCode: 'A3F9K2',
      hostId: 'p1a2b3c4',
      gameMode: 'COOP',
      players: [
        { id: 'p1a2b3c4', name: 'Alice', ready: true },
        { id: 'p2d5e6f7', name: 'Bob', ready: false },
      ],
    })
  })

  it('LOBBY_STATE überschreibt vorherigen Lobby-Zustand', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({
      type: 'LOBBY_STATE',
      data: { lobbyCode: 'A3F9K2', hostId: 'p1a2b3c4', gameMode: 'COOP', players: [] },
    })
    ws.simulateMessage({
      type: 'LOBBY_STATE',
      data: { lobbyCode: 'A3F9K2', hostId: 'p1a2b3c4', gameMode: 'FFA', players: [] },
    })

    expect(store.lobby?.gameMode).toBe('FFA')
  })

  it('DISCONNECTED eines anderen Spielers wird in gameEvents gespeichert', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({ type: 'DISCONNECTED', data: { playerId: 'p2d5e6f7', playerName: 'Bob' } })

    expect(store.gameEvents).toHaveLength(1)
    expect(store.gameEvents[0]).toMatchObject({ playerId: 'p2d5e6f7', playerName: 'Bob' })
  })
})
