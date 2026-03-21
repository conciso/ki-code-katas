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
  let socket: WebSocket | null = null
  let pingSentAt: number | null = null

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

  return { isConnected, playerId, playerName, gameEvents, lastError, latency, lobby, isHost, connect, ping, createLobby }
})
