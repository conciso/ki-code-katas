import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useGameStore = defineStore('game', () => {
  const isConnected = ref(false)
  const playerId = ref<string | null>(null)
  const gameEvents = ref<object[]>([])
  const lastError = ref<{ code: string; message: string } | null>(null)
  const latency = ref<number | null>(null)
  const lobby = ref<object | null>(null)
  let socket: WebSocket | null = null
  let pingSentAt: number | null = null

  function connect(playerName: string) {
    const host = import.meta.env['VITE_WS_HOST'] ?? 'localhost'
    const port = import.meta.env['VITE_WS_PORT'] ?? '8080'
    socket = new WebSocket(`ws://${host}:${port}/ws/game?playerName=${playerName}`)

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

  return { isConnected, playerId, gameEvents, lastError, latency, lobby, connect, ping }
})
