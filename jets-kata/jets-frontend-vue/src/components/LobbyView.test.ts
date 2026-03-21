// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useGameStore } from '@/stores/useGameStore'
import LobbyView from './LobbyView.vue'
import type { Lobby } from '@/types'

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div/>' } },
      { path: '/create', component: { template: '<div/>' } },
      { path: '/game', component: { template: '<div/>' } },
    ],
  })
}

const LOBBY: Lobby = {
  lobbyCode: 'A3F9K2',
  hostId: 'p1a2b3c4',
  gameMode: 'COOP',
  players: [
    { id: 'p1a2b3c4', name: 'Alice', ready: true, color: '#FF4444' },
    { id: 'p2d5e6f7', name: 'Bob', ready: false, color: '#4444FF' },
  ],
}

describe('LobbyView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  function mountWithLobby(overrides: Partial<Lobby> = {}) {
    const store = useGameStore()
    store.lobby = { ...LOBBY, ...overrides }
    store.playerId = 'p1a2b3c4'
    return { wrapper: mount(LobbyView, { global: { plugins: [makeRouter()] } }), store }
  }

  // ── Lobby-Info ─────────────────────────────────────────────────────────

  it('zeigt den Lobby-Code an', () => {
    const { wrapper } = mountWithLobby()
    expect(wrapper.text()).toContain('A3F9K2')
  })

  it('zeigt den Spielmodus an', () => {
    const { wrapper } = mountWithLobby()
    expect(wrapper.text()).toContain('COOP')
  })

  // ── Spielerliste ───────────────────────────────────────────────────────

  it('zeigt alle Spielernamen an', () => {
    const { wrapper } = mountWithLobby()
    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.text()).toContain('Bob')
  })

  it('zeigt die Spielerfarbe als farbigen Indikator', () => {
    const { wrapper } = mountWithLobby()
    const dots = wrapper.findAll('[data-testid="player-color"]')
    expect(dots[0]?.attributes('style')).toContain('rgb(255, 68, 68)')
    expect(dots[1]?.attributes('style')).toContain('rgb(68, 68, 255)')
  })

  it('zeigt den Ready-Status pro Spieler an', () => {
    const { wrapper } = mountWithLobby()
    const items = wrapper.findAll('[data-testid="player-item"]')
    expect(items[0]?.text()).toContain('Bereit')
    expect(items[1]?.text()).toContain('Warten')
  })

  // ── Ready-Button ───────────────────────────────────────────────────────

  it('zeigt einen "Bereit"-Button', () => {
    const store = useGameStore()
    store.lobby = { ...LOBBY, players: [{ id: 'p1a2b3c4', name: 'Alice', ready: false, color: '#FF4444' }] }
    store.playerId = 'p1a2b3c4'
    const wrapper = mount(LobbyView, { global: { plugins: [makeRouter()] } })

    expect(wrapper.find('[data-testid="btn-ready"]').exists()).toBe(true)
  })

  it('ruft setReady(true) auf wenn man nicht bereit ist', async () => {
    const store = useGameStore()
    store.lobby = { ...LOBBY, players: [{ id: 'p1a2b3c4', name: 'Alice', ready: false, color: '#FF4444' }] }
    store.playerId = 'p1a2b3c4'
    vi.spyOn(store, 'setReady').mockImplementation(() => {})
    const wrapper = mount(LobbyView, { global: { plugins: [makeRouter()] } })

    await wrapper.find('[data-testid="btn-ready"]').trigger('click')

    expect(store.setReady).toHaveBeenCalledWith(true)
  })

  it('ruft setReady(false) auf wenn man bereits bereit ist', async () => {
    const store = useGameStore()
    store.lobby = { ...LOBBY, players: [{ id: 'p1a2b3c4', name: 'Alice', ready: true, color: '#FF4444' }] }
    store.playerId = 'p1a2b3c4'
    vi.spyOn(store, 'setReady').mockImplementation(() => {})
    const wrapper = mount(LobbyView, { global: { plugins: [makeRouter()] } })

    await wrapper.find('[data-testid="btn-ready"]').trigger('click')

    expect(store.setReady).toHaveBeenCalledWith(false)
  })

  // ── Host-Aktionen ──────────────────────────────────────────────────────

  it('zeigt "Spiel starten"-Button nur für den Host', () => {
    const { wrapper } = mountWithLobby()
    expect(wrapper.find('[data-testid="btn-start"]').exists()).toBe(true)
  })

  it('zeigt keinen "Spiel starten"-Button für normale Spieler', () => {
    const store = useGameStore()
    store.lobby = LOBBY
    store.playerId = 'p2d5e6f7'
    const wrapper = mount(LobbyView, { global: { plugins: [makeRouter()] } })

    expect(wrapper.find('[data-testid="btn-start"]').exists()).toBe(false)
  })

  it('"Spiel starten"-Button ist deaktiviert wenn nicht alle bereit sind', () => {
    const { wrapper } = mountWithLobby()
    expect(wrapper.find('[data-testid="btn-start"]').attributes('disabled')).toBeDefined()
  })

  it('"Spiel starten"-Button ist aktiv wenn alle bereit sind', () => {
    const { wrapper } = mountWithLobby({
      players: [
        { id: 'p1a2b3c4', name: 'Alice', ready: true, color: '#FF4444' },
        { id: 'p2d5e6f7', name: 'Bob', ready: true, color: '#4444FF' },
      ],
    })
    expect(wrapper.find('[data-testid="btn-start"]').attributes('disabled')).toBeUndefined()
  })

  it('ruft startGame() auf wenn Host auf "Spiel starten" klickt', async () => {
    const { wrapper, store } = mountWithLobby({
      players: [
        { id: 'p1a2b3c4', name: 'Alice', ready: true, color: '#FF4444' },
        { id: 'p2d5e6f7', name: 'Bob', ready: true, color: '#4444FF' },
      ],
    })
    vi.spyOn(store, 'startGame').mockImplementation(() => {})

    await wrapper.find('[data-testid="btn-start"]').trigger('click')

    expect(store.startGame).toHaveBeenCalled()
  })

  // ── Lobby verlassen ────────────────────────────────────────────────────

  it('zeigt einen "Verlassen"-Button', () => {
    const { wrapper } = mountWithLobby()
    expect(wrapper.find('[data-testid="btn-leave"]').exists()).toBe(true)
  })

  it('ruft leaveLobby() auf beim Klick auf "Verlassen"', async () => {
    const { wrapper, store } = mountWithLobby()
    vi.spyOn(store, 'leaveLobby').mockImplementation(() => {})

    await wrapper.find('[data-testid="btn-leave"]').trigger('click')

    expect(store.leaveLobby).toHaveBeenCalled()
  })

  it('navigiert zu /create wenn lobby null wird', async () => {
    const store = useGameStore()
    store.lobby = LOBBY
    store.playerId = 'p1a2b3c4'
    const router = makeRouter()
    vi.spyOn(router, 'push').mockImplementation(async () => {})

    mount(LobbyView, { global: { plugins: [router] } })
    store.lobby = null
    await new Promise((r) => setTimeout(r, 0))

    expect(router.push).toHaveBeenCalledWith('/create')
  })

  it('zeigt "Session beenden"-Button nur für den Host', () => {
    const { wrapper } = mountWithLobby()
    expect(wrapper.find('[data-testid="btn-end-session"]').exists()).toBe(true)
  })

  it('zeigt keinen "Session beenden"-Button für normale Spieler', () => {
    const store = useGameStore()
    store.lobby = LOBBY
    store.playerId = 'p2d5e6f7'
    const wrapper = mount(LobbyView, { global: { plugins: [makeRouter()] } })
    expect(wrapper.find('[data-testid="btn-end-session"]').exists()).toBe(false)
  })

  it('ruft leaveLobby() auf beim Klick auf "Session beenden"', async () => {
    const { wrapper, store } = mountWithLobby()
    vi.spyOn(store, 'leaveLobby').mockImplementation(() => {})

    await wrapper.find('[data-testid="btn-end-session"]').trigger('click')

    expect(store.leaveLobby).toHaveBeenCalled()
  })
})
