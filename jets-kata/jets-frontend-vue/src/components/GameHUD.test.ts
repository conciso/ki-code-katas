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

  it('hat ein Element mit data-testid="hud-wave" das die Wellennummer enthält', () => {
    const store = useGameStore()
    store.gameState = makeGameState({ wave: 5 })

    const wrapper = mount(GameHUD)
    expect(wrapper.find('[data-testid="hud-wave"]').text()).toContain('5')
  })

  it('hat ein Element mit data-testid="hud-hp" das die eigene HP enthält', () => {
    const store = useGameStore()
    store.playerId = 'p1'
    store.gameState = makeGameState()

    const wrapper = mount(GameHUD)
    expect(wrapper.find('[data-testid="hud-hp"]').text()).toContain('3')
  })

  it('hat ein Element mit data-testid="hud-score" das den eigenen Score enthält', () => {
    const store = useGameStore()
    store.playerId = 'p1'
    store.gameState = makeGameState()

    const wrapper = mount(GameHUD)
    expect(wrapper.find('[data-testid="hud-score"]').text()).toContain('1200')
  })

  it('hat ein Element mit data-testid="hud-enemies" das die Gegnerzahl enthält', () => {
    const store = useGameStore()
    store.gameState = makeGameState({ enemiesRemaining: 12 })

    const wrapper = mount(GameHUD)
    expect(wrapper.find('[data-testid="hud-enemies"]').text()).toContain('12')
  })

  it('zeigt Scores aller Spieler im Scoreboard', () => {
    const store = useGameStore()
    store.gameState = makeGameState({
      players: [
        { id: 'p1', x: 0, y: 0, hp: 3, score: 1200, alive: true, respawnIn: 0, invincible: false, activePowerUp: null, lastProcessedInput: 0 },
        { id: 'p2', x: 0, y: 0, hp: 2, score: 800, alive: true, respawnIn: 0, invincible: false, activePowerUp: null, lastProcessedInput: 0 },
      ],
    })

    const wrapper = mount(GameHUD)
    const scores = wrapper.findAll('[data-testid="hud-player-score"]')
    expect(scores).toHaveLength(2)
    expect(scores[0]!.text()).toContain('1200')
    expect(scores[1]!.text()).toContain('800')
  })

  it('zeigt Respawn-Countdown wenn eigener Spieler tot ist', () => {
    const store = useGameStore()
    store.playerId = 'p1'
    store.gameState = makeGameState({
      players: [
        { id: 'p1', x: 0, y: 0, hp: 0, score: 0, alive: false, respawnIn: 3, invincible: false, activePowerUp: null, lastProcessedInput: 0 },
      ],
    })

    const wrapper = mount(GameHUD)
    expect(wrapper.find('[data-testid="hud-respawn"]').text()).toContain('3')
  })

  it('aktualisiert sich reaktiv wenn sich gameState ändert', async () => {
    const store = useGameStore()
    store.playerId = 'p1'
    store.gameState = makeGameState({ wave: 1 })

    const wrapper = mount(GameHUD)
    expect(wrapper.find('[data-testid="hud-wave"]').text()).toContain('1')

    store.gameState = makeGameState({ wave: 2 })
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-testid="hud-wave"]').text()).toContain('2')
  })
})
