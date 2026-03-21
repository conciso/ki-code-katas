import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Lobby, GameState, GameStarting, WaveComplete, GameOver } from '@/types'

export const useGameStore = defineStore('game', () => {
  const isConnected = ref(false)
  const playerId = ref<string | null>(null)
  const playerName = ref<string | null>(null)
  const gameEvents = ref<Record<string, unknown>[]>([])
  const lastError = ref<{ code: string; message: string } | null>(null)
  const latency = ref<number | null>(null)
  const lobby = ref<Lobby | null>(null)
  const isHost = computed(() => !!lobby.value && lobby.value.hostId === playerId.value)
  const gameStarting = ref<GameStarting | null>(null)
  const gameState = ref<GameState | null>(null)
  const waveComplete = ref<WaveComplete | null>(null)
  const gameOver = ref<GameOver | null>(null)
  let socket: WebSocket | null = null
  let pingSentAt: number | null = null
  let inputSeq = 0
  const sendQueue: string[] = []

  function send(msg: object) {
    const data = JSON.stringify(msg)
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(data)
    } else {
      sendQueue.push(data)
    }
  }

  function connect(name: string) {
    playerName.value = name
    const host = import.meta.env['VITE_WS_HOST'] ?? 'localhost'
    const port = import.meta.env['VITE_WS_PORT'] ?? '8080'
    socket = new WebSocket(`ws://${host}:${port}/ws/game?playerName=${name}`)

    socket.onopen = () => {
      let queued: string | undefined
      while ((queued = sendQueue.shift()) !== undefined) {
        socket!.send(queued)
      }
    }

    socket.onmessage = (event: MessageEvent) => {
      const msg = JSON.parse(event.data)

      if (msg.type === 'CONNECTED') {
        isConnected.value = true
        playerId.value = msg.data.playerId
      } else if (msg.type === 'GAME_EVENT') {
        gameEvents.value.push(msg.data)
      } else if (msg.type === 'ERROR') {
        lastError.value = msg.data
      } else if (msg.type === 'PONG' && pingSentAt !== null) {
        latency.value = Date.now() - pingSentAt
        pingSentAt = null
      } else if (msg.type === 'LOBBY_CREATED' || msg.type === 'LOBBY_STATE') {
        lobby.value = msg.data
      } else if (msg.type === 'DISCONNECTED') {
        gameEvents.value.push(msg.data)
      } else if (msg.type === 'GAME_STARTING') {
        gameStarting.value = msg.data
      } else if (msg.type === 'GAME_STATE') {
        gameState.value = msg.data
      } else if (msg.type === 'WAVE_COMPLETE') {
        waveComplete.value = msg.data
      } else if (msg.type === 'GAME_OVER') {
        gameOver.value = msg.data
      }
    }

    socket.onclose = () => {
      isConnected.value = false
    }
  }

  function ping() {
    pingSentAt = Date.now()
    send({ type: 'PING', data: { timestamp: pingSentAt } })
  }

  function createLobby() {
    send({ type: 'CREATE_LOBBY', data: { playerName: playerName.value } })
  }

  function joinLobby(lobbyCode: string) {
    send({ type: 'JOIN_LOBBY', data: { lobbyCode, playerName: playerName.value } })
  }

  function setGameMode(gameMode: 'COOP' | 'FFA') {
    send({ type: 'SET_GAME_MODE', data: { gameMode } })
  }

  function setReady(ready: boolean) {
    send({ type: 'PLAYER_READY', data: { ready } })
  }

  function startGame() {
    send({ type: 'START_GAME', data: {} })
  }

  function leaveLobby() {
    send({ type: 'LEAVE_LOBBY', data: {} })
    lobby.value = null
  }

  function returnToLobby() {
    send({ type: 'RETURN_TO_LOBBY', data: {} })
    gameOver.value = null
  }

  function sendInput(input: { up: boolean; down: boolean; left: boolean; right: boolean; shoot: boolean }) {
    inputSeq += 1
    send({ type: 'PLAYER_INPUT', data: { ...input, seq: inputSeq } })
  }

  return {
    isConnected, playerId, playerName, gameEvents, lastError, latency,
    lobby, isHost, gameStarting, gameState, waveComplete, gameOver,
    connect, ping, createLobby, joinLobby, setGameMode, setReady,
    startGame, leaveLobby, returnToLobby, sendInput,
  }
})
