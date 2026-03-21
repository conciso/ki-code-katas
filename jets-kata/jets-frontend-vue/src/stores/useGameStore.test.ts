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
  readyState: number = WebSocket.CONNECTING

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

describe('useGameStore — Lobby erstellen', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  function connectAndOpen(store: ReturnType<typeof useGameStore>, playerName = 'Alice') {
    store.connect(playerName)
    const ws = MockWebSocket.instances[0]!
    ws.simulateOpen()
    ws.simulateMessage({ type: 'CONNECTED', data: { playerId: 'p1a2b3c4', serverTickRate: 30 } })
    return ws
  }

  it('isHost ist initial false', () => {
    const store = useGameStore()
    expect(store.isHost).toBe(false)
  })

  it('createLobby() sendet CREATE_LOBBY mit dem Spielernamen', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store, 'Alice')

    store.createLobby()

    expect(ws.send).toHaveBeenCalledWith(
      JSON.stringify({ type: 'CREATE_LOBBY', data: { playerName: 'Alice' } }),
    )
  })

  it('isHost ist true wenn eigene playerId dem hostId entspricht', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({ type: 'LOBBY_CREATED', data: { lobbyCode: 'A3F9K2', hostId: 'p1a2b3c4' } })

    expect(store.isHost).toBe(true)
  })

  it('isHost ist false wenn ein anderer Spieler Host ist', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({ type: 'LOBBY_CREATED', data: { lobbyCode: 'A3F9K2', hostId: 'p9anderer' } })

    expect(store.isHost).toBe(false)
  })

  it('isHost bleibt aktuell nach LOBBY_STATE (z.B. Host-Wechsel)', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store)

    ws.simulateMessage({ type: 'LOBBY_CREATED', data: { lobbyCode: 'A3F9K2', hostId: 'p1a2b3c4' } })
    ws.simulateMessage({
      type: 'LOBBY_STATE',
      data: { lobbyCode: 'A3F9K2', hostId: 'p2d5e6f7', gameMode: 'COOP', players: [] },
    })

    expect(store.isHost).toBe(false)
  })
})

describe('useGameStore — Lobby beitreten', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  function connectAndOpen(store: ReturnType<typeof useGameStore>, playerName = 'Bob') {
    store.connect(playerName)
    const ws = MockWebSocket.instances[0]!
    ws.simulateOpen()
    ws.simulateMessage({ type: 'CONNECTED', data: { playerId: 'p2d5e6f7', serverTickRate: 30 } })
    return ws
  }

  it('joinLobby() sendet JOIN_LOBBY mit lobbyCode und Spielernamen', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store, 'Bob')

    store.joinLobby('A3F9K2')

    expect(ws.send).toHaveBeenCalledWith(
      JSON.stringify({ type: 'JOIN_LOBBY', data: { lobbyCode: 'A3F9K2', playerName: 'Bob' } }),
    )
  })

  it('isHost ist false nach dem Beitreten', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store, 'Bob')

    store.joinLobby('A3F9K2')
    ws.simulateMessage({
      type: 'LOBBY_STATE',
      data: { lobbyCode: 'A3F9K2', hostId: 'p1a2b3c4', gameMode: 'COOP', players: [] },
    })

    expect(store.isHost).toBe(false)
  })

  it('lobby enthält den Zustand nach LOBBY_STATE', () => {
    const store = useGameStore()
    const ws = connectAndOpen(store, 'Bob')

    store.joinLobby('A3F9K2')
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
      players: [{ name: 'Alice' }, { name: 'Bob' }],
    })
  })
})

// ─── Hilfsfunktion für alle folgenden Suites ───────────────────────────────

function makeConnectedStore() {
  const store = useGameStore()
  store.connect('Alice')
  const ws = MockWebSocket.instances[0]!
  ws.simulateOpen()
  ws.simulateMessage({ type: 'CONNECTED', data: { playerId: 'p1a2b3c4', serverTickRate: 30 } })
  return { store, ws }
}

// ─── Lobby-Aktionen ────────────────────────────────────────────────────────

describe('useGameStore — setGameMode', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  it('sendet SET_GAME_MODE mit dem gewählten Modus', () => {
    const { store, ws } = makeConnectedStore()

    store.setGameMode('FFA')

    expect(ws.send).toHaveBeenCalledWith(
      JSON.stringify({ type: 'SET_GAME_MODE', data: { gameMode: 'FFA' } }),
    )
  })

  it('sendet SET_GAME_MODE mit COOP', () => {
    const { store, ws } = makeConnectedStore()

    store.setGameMode('COOP')

    expect(ws.send).toHaveBeenCalledWith(
      JSON.stringify({ type: 'SET_GAME_MODE', data: { gameMode: 'COOP' } }),
    )
  })
})

describe('useGameStore — setReady', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  it('sendet PLAYER_READY mit ready=true', () => {
    const { store, ws } = makeConnectedStore()

    store.setReady(true)

    expect(ws.send).toHaveBeenCalledWith(
      JSON.stringify({ type: 'PLAYER_READY', data: { ready: true } }),
    )
  })

  it('sendet PLAYER_READY mit ready=false', () => {
    const { store, ws } = makeConnectedStore()

    store.setReady(false)

    expect(ws.send).toHaveBeenCalledWith(
      JSON.stringify({ type: 'PLAYER_READY', data: { ready: false } }),
    )
  })
})

describe('useGameStore — startGame', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  it('sendet START_GAME', () => {
    const { store, ws } = makeConnectedStore()

    store.startGame()

    expect(ws.send).toHaveBeenCalledWith(
      JSON.stringify({ type: 'START_GAME', data: {} }),
    )
  })
})

describe('useGameStore — leaveLobby', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  it('sendet LEAVE_LOBBY', () => {
    const { store, ws } = makeConnectedStore()

    store.leaveLobby()

    expect(ws.send).toHaveBeenCalledWith(
      JSON.stringify({ type: 'LEAVE_LOBBY', data: {} }),
    )
  })

  it('setzt lobby auf null', () => {
    const { store, ws } = makeConnectedStore()
    ws.simulateMessage({ type: 'LOBBY_CREATED', data: { lobbyCode: 'A3F9K2', hostId: 'p1a2b3c4' } })

    store.leaveLobby()

    expect(store.lobby).toBeNull()
  })
})

describe('useGameStore — returnToLobby', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  it('sendet RETURN_TO_LOBBY', () => {
    const { store, ws } = makeConnectedStore()

    store.returnToLobby()

    expect(ws.send).toHaveBeenCalledWith(
      JSON.stringify({ type: 'RETURN_TO_LOBBY', data: {} }),
    )
  })
})

// ─── Spielaktionen ─────────────────────────────────────────────────────────

describe('useGameStore — sendInput', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  it('sendet PLAYER_INPUT mit dem aktuellen Input-Zustand', () => {
    const { store, ws } = makeConnectedStore()

    store.sendInput({ up: false, down: false, left: true, right: false, shoot: true })

    expect(ws.send).toHaveBeenCalledWith(
      JSON.stringify({ type: 'PLAYER_INPUT', data: { up: false, down: false, left: true, right: false, shoot: true, seq: 1 } }),
    )
  })

  it('erhöht die Sequenznummer bei jedem Input', () => {
    const { store, ws } = makeConnectedStore()

    store.sendInput({ up: true, down: false, left: false, right: false, shoot: false })
    store.sendInput({ up: false, down: false, left: false, right: true, shoot: false })

    const calls = ws.send.mock.calls
    expect(JSON.parse(calls[0]![0]).data.seq).toBe(1)
    expect(JSON.parse(calls[1]![0]).data.seq).toBe(2)
  })
})

// ─── Eingehende Spielnachrichten ───────────────────────────────────────────

describe('useGameStore — GAME_STARTING', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  it('gameStarting ist initial null', () => {
    const store = useGameStore()
    expect(store.gameStarting).toBeNull()
  })

  it('speichert GAME_STARTING-Daten', () => {
    const { store, ws } = makeConnectedStore()

    ws.simulateMessage({
      type: 'GAME_STARTING',
      data: { countdown: 3, gameMode: 'COOP', fieldWidth: 1920, fieldHeight: 1080, players: [] },
    })

    expect(store.gameStarting).toMatchObject({ countdown: 3, gameMode: 'COOP' })
  })
})

describe('useGameStore — GAME_STATE', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  it('gameState ist initial null', () => {
    const store = useGameStore()
    expect(store.gameState).toBeNull()
  })

  it('speichert den letzten GAME_STATE', () => {
    const { store, ws } = makeConnectedStore()

    ws.simulateMessage({
      type: 'GAME_STATE',
      data: {
        tick: 1847,
        players: [{ id: 'p1a2b3c4', x: 450.5, y: 320.0, hp: 3, score: 1200, alive: true }],
        projectiles: [],
        enemies: [],
        powerUps: [],
        wave: 3,
        enemiesRemaining: 8,
      },
    })

    expect(store.gameState).toMatchObject({ tick: 1847, wave: 3 })
  })

  it('überschreibt den vorherigen GAME_STATE', () => {
    const { store, ws } = makeConnectedStore()

    ws.simulateMessage({ type: 'GAME_STATE', data: { tick: 1, players: [], projectiles: [], enemies: [], powerUps: [], wave: 1, enemiesRemaining: 5 } })
    ws.simulateMessage({ type: 'GAME_STATE', data: { tick: 2, players: [], projectiles: [], enemies: [], powerUps: [], wave: 1, enemiesRemaining: 4 } })

    expect(store.gameState?.tick).toBe(2)
  })
})

describe('useGameStore — WAVE_COMPLETE', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  it('waveComplete ist initial null', () => {
    const store = useGameStore()
    expect(store.waveComplete).toBeNull()
  })

  it('speichert WAVE_COMPLETE-Daten', () => {
    const { store, ws } = makeConnectedStore()

    ws.simulateMessage({
      type: 'WAVE_COMPLETE',
      data: { wave: 3, nextWaveIn: 5, scores: { p1a2b3c4: 1200, p2d5e6f7: 800 } },
    })

    expect(store.waveComplete).toMatchObject({ wave: 3, nextWaveIn: 5 })
  })
})

describe('useGameStore — GAME_OVER', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  it('gameOver ist initial null', () => {
    const store = useGameStore()
    expect(store.gameOver).toBeNull()
  })

  it('speichert GAME_OVER-Daten', () => {
    const { store, ws } = makeConnectedStore()

    ws.simulateMessage({
      type: 'GAME_OVER',
      data: {
        reason: 'ALL_DEAD',
        finalScores: [{ playerId: 'p1a2b3c4', name: 'Alice', score: 4500, kills: 32 }],
        totalScore: 4500,
        wavesCompleted: 7,
      },
    })

    expect(store.gameOver).toMatchObject({ reason: 'ALL_DEAD', wavesCompleted: 7 })
  })

  it('setzt gameOver auf null wenn returnToLobby aufgerufen wird', () => {
    const { store, ws } = makeConnectedStore()
    ws.simulateMessage({
      type: 'GAME_OVER',
      data: { reason: 'ALL_DEAD', finalScores: [], totalScore: 0, wavesCompleted: 1 },
    })

    store.returnToLobby()

    expect(store.gameOver).toBeNull()
  })
})
