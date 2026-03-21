// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { useGameStore } from '@/stores/useGameStore'
import LobbyCreate from './LobbyCreate.vue'

describe('LobbyCreate', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('rendert ein Eingabefeld für den Spielernamen', () => {
    const wrapper = mount(LobbyCreate)

    expect(wrapper.find('input[placeholder]').exists()).toBe(true)
  })

  it('rendert einen "Lobby erstellen"-Button', () => {
    const wrapper = mount(LobbyCreate)

    expect(wrapper.find('button').text()).toContain('Lobby erstellen')
  })

  it('Button ist deaktiviert solange kein Name eingegeben', async () => {
    const wrapper = mount(LobbyCreate)

    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })

  it('Button ist aktiv wenn ein Name eingegeben wurde', async () => {
    const wrapper = mount(LobbyCreate)

    await wrapper.find('input').setValue('Alice')

    expect(wrapper.find('button').attributes('disabled')).toBeUndefined()
  })

  it('ruft connect() und createLobby() beim Klick auf', async () => {
    const store = useGameStore()
    vi.spyOn(store, 'connect').mockImplementation(() => {})
    vi.spyOn(store, 'createLobby').mockImplementation(() => {})

    const wrapper = mount(LobbyCreate)
    await wrapper.find('input').setValue('Alice')
    await wrapper.find('button').trigger('click')

    expect(store.connect).toHaveBeenCalledWith('Alice')
    expect(store.createLobby).toHaveBeenCalled()
  })

  it('zeigt den Lobby-Code an sobald die Lobby erstellt wurde', async () => {
    const store = useGameStore()
    vi.spyOn(store, 'connect').mockImplementation(() => {})
    vi.spyOn(store, 'createLobby').mockImplementation(() => {
      store.lobby = { lobbyCode: 'A3F9K2', hostId: 'p1a2b3c4', gameMode: 'COOP', players: [] }
    })

    const wrapper = mount(LobbyCreate)
    await wrapper.find('input').setValue('Alice')
    await wrapper.find('button').trigger('click')

    expect(wrapper.text()).toContain('A3F9K2')
  })

  it('zeigt keinen Lobby-Code vor der Erstellung', () => {
    const wrapper = mount(LobbyCreate)

    expect(wrapper.text()).not.toContain('Lobby-Code')
  })
})
