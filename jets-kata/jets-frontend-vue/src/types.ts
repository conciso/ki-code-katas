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
