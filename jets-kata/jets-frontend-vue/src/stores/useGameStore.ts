import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useGameStore = defineStore('game', () => {
  const isConnected = ref(false)
  const playerId = ref<string | null>(null)
  const playerName = ref<string | null>(null)
  const gameEvents = ref<Record<string, unknown>[]>([])
  const lastError = ref<{ code: string; message: string } | null>(null)
  const latency = ref<number | null>(null)
  const lobby = ref<{ hostId: string; [key: string]: unknown } | null>(null)
  const isHost = computed(() => !!lobby.value && lobby.value.hostId === playerId.value)
  const gameStarting = ref<Record<string, unknown> | null>(null)
  const gameState = ref<Record<string, unknown> | null>(null)
  const waveComplete = ref<Record<string, unknown> | null>(null)
  const gameOver = ref<Record<string, unknown> | null>(null)
  let socket: WebSocket | null = null
  let pingSentAt: number | null = null
  let inputSeq = 0

  function connect(name: string) {
    playerName.value = name
    const host = import.meta.env['VITE_WS_HOST'] ?? 'localhost'
    const port = import.meta.env['VITE_WS_PORT'] ?? '8080'
    socket = new WebSocket(`ws://${host}:${port}/ws/game?playerName=${name}`)

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
    socket?.send(JSON.stringify({ type: 'PING', data: { timestamp: pingSentAt } }))
  }

  function createLobby() {
    socket?.send(JSON.stringify({ type: 'CREATE_LOBBY', data: { playerName: playerName.value } }))
  }

  function joinLobby(lobbyCode: string) {
    socket?.send(JSON.stringify({ type: 'JOIN_LOBBY', data: { lobbyCode, playerName: playerName.value } }))
  }

  function setGameMode(gameMode: 'COOP' | 'FFA') {
    socket?.send(JSON.stringify({ type: 'SET_GAME_MODE', data: { gameMode } }))
  }

  function setReady(ready: boolean) {
    socket?.send(JSON.stringify({ type: 'PLAYER_READY', data: { ready } }))
  }

  function startGame() {
    socket?.send(JSON.stringify({ type: 'START_GAME', data: {} }))
  }

  function leaveLobby() {
    socket?.send(JSON.stringify({ type: 'LEAVE_LOBBY', data: {} }))
    lobby.value = null
  }

  function returnToLobby() {
    socket?.send(JSON.stringify({ type: 'RETURN_TO_LOBBY', data: {} }))
    gameOver.value = null
  }

  function sendInput(input: { up: boolean; down: boolean; left: boolean; right: boolean; shoot: boolean }) {
    inputSeq += 1
    socket?.send(JSON.stringify({ type: 'PLAYER_INPUT', data: { ...input, seq: inputSeq } }))
  }

  return {
    isConnected, playerId, playerName, gameEvents, lastError, latency,
    lobby, isHost, gameStarting, gameState, waveComplete, gameOver,
    connect, ping, createLobby, joinLobby, setGameMode, setReady,
    startGame, leaveLobby, returnToLobby, sendInput,
  }
})
