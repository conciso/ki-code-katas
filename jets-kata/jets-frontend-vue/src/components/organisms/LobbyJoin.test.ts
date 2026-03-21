// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useGameStore } from '@/stores/useGameStore'
import LobbyJoin from './LobbyJoin.vue'

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div/>' } },
      { path: '/lobby', component: { template: '<div/>' } },
    ],
  })
}

describe('LobbyJoin', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('rendert ein Eingabefeld für den Spielernamen', () => {
    const wrapper = mount(LobbyJoin, { global: { plugins: [makeRouter()] } })
    expect(wrapper.find('input[data-testid="player-name"]').exists()).toBe(true)
  })

  it('rendert ein Eingabefeld für den Lobby-Code', () => {
    const wrapper = mount(LobbyJoin, { global: { plugins: [makeRouter()] } })
    expect(wrapper.find('input[data-testid="lobby-code"]').exists()).toBe(true)
  })

  it('rendert einen "Beitreten"-Button', () => {
    const wrapper = mount(LobbyJoin, { global: { plugins: [makeRouter()] } })
    expect(wrapper.find('button').text()).toContain('Beitreten')
  })

  it('Button ist deaktiviert wenn beide Felder leer sind', () => {
    const wrapper = mount(LobbyJoin, { global: { plugins: [makeRouter()] } })
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })

  it('Button ist deaktiviert wenn nur der Name fehlt', async () => {
    const wrapper = mount(LobbyJoin, { global: { plugins: [makeRouter()] } })
    await wrapper.find('input[data-testid="lobby-code"]').setValue('A3F9K2')
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })

  it('Button ist deaktiviert wenn nur der Lobby-Code fehlt', async () => {
    const wrapper = mount(LobbyJoin, { global: { plugins: [makeRouter()] } })
    await wrapper.find('input[data-testid="player-name"]').setValue('Bob')
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })

  it('Button ist aktiv wenn Name und Lobby-Code eingegeben sind', async () => {
    const wrapper = mount(LobbyJoin, { global: { plugins: [makeRouter()] } })
    await wrapper.find('input[data-testid="player-name"]').setValue('Bob')
    await wrapper.find('input[data-testid="lobby-code"]').setValue('A3F9K2')
    expect(wrapper.find('button').attributes('disabled')).toBeUndefined()
  })

  it('ruft connect() und joinLobby() beim Klick auf', async () => {
    const store = useGameStore()
    vi.spyOn(store, 'connect').mockImplementation(() => {})
    vi.spyOn(store, 'joinLobby').mockImplementation(() => {})

    const wrapper = mount(LobbyJoin, { global: { plugins: [makeRouter()] } })
    await wrapper.find('input[data-testid="player-name"]').setValue('Bob')
    await wrapper.find('input[data-testid="lobby-code"]').setValue('A3F9K2')
    await wrapper.find('button').trigger('click')

    expect(store.connect).toHaveBeenCalledWith('Bob')
    expect(store.joinLobby).toHaveBeenCalledWith('A3F9K2')
  })

  it('navigiert zu /lobby nach erfolgreichem Beitritt', async () => {
    const store = useGameStore()
    const router = makeRouter()
    vi.spyOn(store, 'connect').mockImplementation(() => {})
    vi.spyOn(store, 'joinLobby').mockImplementation(() => {
      store.lobby = { lobbyCode: 'A3F9K2', hostId: 'p1a2b3c4', gameMode: 'COOP', players: [] }
    })
    vi.spyOn(router, 'push').mockImplementation(async () => {})

    const wrapper = mount(LobbyJoin, { global: { plugins: [router] } })
    await wrapper.find('input[data-testid="player-name"]').setValue('Bob')
    await wrapper.find('input[data-testid="lobby-code"]').setValue('A3F9K2')
    await wrapper.find('button').trigger('click')
    await wrapper.vm.$nextTick()

    expect(router.push).toHaveBeenCalledWith('/lobby')
  })

  it('zeigt keine Spielerliste vor dem Beitritt', () => {
    const wrapper = mount(LobbyJoin, { global: { plugins: [makeRouter()] } })
    expect(wrapper.text()).not.toContain('Alice')
  })
})
