// @vitest-environment jsdom
import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { useGameStore } from '@/stores/useGameStore'
import LobbyError from './LobbyError.vue'

describe('LobbyError', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('zeigt nichts wenn kein Fehler vorliegt', () => {
    const wrapper = mount(LobbyError)
    expect(wrapper.text()).toBe('')
  })

  it('zeigt eine Fehlermeldung bei LOBBY_FULL', () => {
    const store = useGameStore()
    store.lastError = { code: 'LOBBY_FULL', message: 'Die Lobby ist voll (max. 4 Spieler)' }
    const wrapper = mount(LobbyError)

    expect(wrapper.text()).toContain('Die Lobby ist voll')
  })

  it('zeigt eine Fehlermeldung bei LOBBY_NOT_FOUND', () => {
    const store = useGameStore()
    store.lastError = { code: 'LOBBY_NOT_FOUND', message: 'Lobby-Code existiert nicht' }
    const wrapper = mount(LobbyError)

    expect(wrapper.text()).toContain('Lobby-Code existiert nicht')
  })

  it('zeigt eine Fehlermeldung bei GAME_IN_PROGRESS', () => {
    const store = useGameStore()
    store.lastError = { code: 'GAME_IN_PROGRESS', message: 'Spiel hat bereits begonnen' }
    const wrapper = mount(LobbyError)

    expect(wrapper.text()).toContain('Spiel hat bereits begonnen')
  })

  it('zeigt den Fehlercode an', () => {
    const store = useGameStore()
    store.lastError = { code: 'LOBBY_FULL', message: 'Die Lobby ist voll (max. 4 Spieler)' }
    const wrapper = mount(LobbyError)

    expect(wrapper.text()).toContain('LOBBY_FULL')
  })
})
