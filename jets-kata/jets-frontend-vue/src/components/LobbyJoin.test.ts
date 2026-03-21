// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { useGameStore } from '@/stores/useGameStore'
import LobbyJoin from './LobbyJoin.vue'

describe('LobbyJoin', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('rendert ein Eingabefeld für den Spielernamen', () => {
    const wrapper = mount(LobbyJoin)

    expect(wrapper.find('input[data-testid="player-name"]').exists()).toBe(true)
  })

  it('rendert ein Eingabefeld für den Lobby-Code', () => {
    const wrapper = mount(LobbyJoin)

    expect(wrapper.find('input[data-testid="lobby-code"]').exists()).toBe(true)
  })

  it('rendert einen "Beitreten"-Button', () => {
    const wrapper = mount(LobbyJoin)

    expect(wrapper.find('button').text()).toContain('Beitreten')
  })

  it('Button ist deaktiviert wenn beide Felder leer sind', () => {
    const wrapper = mount(LobbyJoin)

    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })

  it('Button ist deaktiviert wenn nur der Name fehlt', async () => {
    const wrapper = mount(LobbyJoin)

    await wrapper.find('input[data-testid="lobby-code"]').setValue('A3F9K2')

    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })

  it('Button ist deaktiviert wenn nur der Lobby-Code fehlt', async () => {
    const wrapper = mount(LobbyJoin)

    await wrapper.find('input[data-testid="player-name"]').setValue('Bob')

    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })

  it('Button ist aktiv wenn Name und Lobby-Code eingegeben sind', async () => {
    const wrapper = mount(LobbyJoin)

    await wrapper.find('input[data-testid="player-name"]').setValue('Bob')
    await wrapper.find('input[data-testid="lobby-code"]').setValue('A3F9K2')

    expect(wrapper.find('button').attributes('disabled')).toBeUndefined()
  })

  it('ruft connect() und joinLobby() beim Klick auf', async () => {
    const store = useGameStore()
    vi.spyOn(store, 'connect').mockImplementation(() => {})
    vi.spyOn(store, 'joinLobby').mockImplementation(() => {})

    const wrapper = mount(LobbyJoin)
    await wrapper.find('input[data-testid="player-name"]').setValue('Bob')
    await wrapper.find('input[data-testid="lobby-code"]').setValue('A3F9K2')
    await wrapper.find('button').trigger('click')

    expect(store.connect).toHaveBeenCalledWith('Bob')
    expect(store.joinLobby).toHaveBeenCalledWith('A3F9K2')
  })

  it('zeigt die Lobby-Spieler an nach erfolgreichem Beitritt', async () => {
    const store = useGameStore()
    vi.spyOn(store, 'connect').mockImplementation(() => {})
    vi.spyOn(store, 'joinLobby').mockImplementation(() => {
      store.lobby = {
        lobbyCode: 'A3F9K2',
        hostId: 'p1a2b3c4',
        gameMode: 'COOP',
        players: [
          { id: 'p1a2b3c4', name: 'Alice', ready: true, color: '#FF4444' },
          { id: 'p2d5e6f7', name: 'Bob', ready: false, color: '#4444FF' },
        ],
      }
    })

    const wrapper = mount(LobbyJoin)
    await wrapper.find('input[data-testid="player-name"]').setValue('Bob')
    await wrapper.find('input[data-testid="lobby-code"]').setValue('A3F9K2')
    await wrapper.find('button').trigger('click')

    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.text()).toContain('Bob')
  })

  it('zeigt keine Spielerliste vor dem Beitritt', () => {
    const wrapper = mount(LobbyJoin)

    expect(wrapper.text()).not.toContain('Alice')
  })
})
