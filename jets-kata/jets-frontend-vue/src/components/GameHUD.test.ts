// @vitest-environment jsdom
import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { useGameStore } from '@/stores/useGameStore'
import GameHUD from './GameHUD.vue'

function makeGameState(overrides = {}) {
  return {
    tick: 1,
    players: [
      {
        id: 'p1',
        x: 100,
        y: 100,
        hp: 3,
        score: 1200,
        alive: true,
        respawnIn: 0,
        invincible: false,
        activePowerUp: null,
        lastProcessedInput: 0,
      },
    ],
    projectiles: [],
    enemies: [],
    powerUps: [],
    wave: 3,
    enemiesRemaining: 8,
    ...overrides,
  }
}

describe('GameHUD', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('zeigt nichts wenn kein gameState vorhanden', () => {
    const wrapper = mount(GameHUD)
    expect(wrapper.text()).toBe('')
  })

  it('zeigt die aktuelle Wellennummer', async () => {
    const store = useGameStore()
    store.gameState = makeGameState({ wave: 3 })

    const wrapper = mount(GameHUD)
    expect(wrapper.text()).toContain('3')
  })

  it('zeigt die verbleibenden Gegner', async () => {
    const store = useGameStore()
    store.gameState = makeGameState({ enemiesRemaining: 8 })

    const wrapper = mount(GameHUD)
    expect(wrapper.text()).toContain('8')
  })

  it('zeigt die HP des eigenen Spielers', async () => {
    const store = useGameStore()
    store.playerId = 'p1'
    store.gameState = makeGameState()

    const wrapper = mount(GameHUD)
    expect(wrapper.text()).toContain('3')
  })

  it('zeigt den Score des eigenen Spielers', async () => {
    const store = useGameStore()
    store.playerId = 'p1'
    store.gameState = makeGameState()

    const wrapper = mount(GameHUD)
    expect(wrapper.text()).toContain('1200')
  })

  it('zeigt die Latenz wenn vorhanden', async () => {
    const store = useGameStore()
    store.latency = 42
    store.gameState = makeGameState()

    const wrapper = mount(GameHUD)
    expect(wrapper.text()).toContain('42')
  })
})
