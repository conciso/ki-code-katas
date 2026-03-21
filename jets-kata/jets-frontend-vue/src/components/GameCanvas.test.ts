// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { useGameStore } from '@/stores/useGameStore'
import GameCanvas from './GameCanvas.vue'

function makeMockCtx() {
  const fillStyleValues: string[] = []
  return {
    clearRect: vi.fn(),
    fillRect: vi.fn(),
    beginPath: vi.fn(),
    arc: vi.fn(),
    fill: vi.fn(),
    save: vi.fn(),
    restore: vi.fn(),
    translate: vi.fn(),
    rotate: vi.fn(),
    get fillStyle() {
      return fillStyleValues[fillStyleValues.length - 1] ?? ''
    },
    set fillStyle(v: string) {
      fillStyleValues.push(v)
    },
    get _fillStyleValues() {
      return fillStyleValues
    },
  }
}

type MockCtx = ReturnType<typeof makeMockCtx>

describe('GameCanvas', () => {
  let mockCtx: MockCtx

  beforeEach(() => {
    setActivePinia(createPinia())
    mockCtx = makeMockCtx()
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(
      mockCtx as unknown as CanvasRenderingContext2D,
    )
  })

  it('rendert ein <canvas> Element', () => {
    const wrapper = mount(GameCanvas)
    expect(wrapper.find('canvas').exists()).toBe(true)
  })

  it('Canvas hat data-testid="game-canvas"', () => {
    const wrapper = mount(GameCanvas)
    expect(wrapper.find('canvas[data-testid="game-canvas"]').exists()).toBe(true)
  })

  it('clearRect wird aufgerufen wenn gameState gesetzt wird', async () => {
    const store = useGameStore()
    const wrapper = mount(GameCanvas)

    store.gameState = {
      tick: 1,
      players: [],
      projectiles: [],
      enemies: [],
      powerUps: [],
      wave: 1,
      enemiesRemaining: 0,
    }
    await wrapper.vm.$nextTick()

    expect(mockCtx.clearRect).toHaveBeenCalled()
  })

  it('zeichnet lebende Spieler an ihrer x/y Position', async () => {
    const store = useGameStore()
    const wrapper = mount(GameCanvas)

    store.gameState = {
      tick: 1,
      players: [{ id: 'p1', x: 450.5, y: 320.0, hp: 3, score: 0, alive: true, respawnIn: 0, invincible: false, activePowerUp: null, lastProcessedInput: 0 }],
      projectiles: [],
      enemies: [],
      powerUps: [],
      wave: 1,
      enemiesRemaining: 0,
    }
    await wrapper.vm.$nextTick()

    expect(mockCtx.arc).toHaveBeenCalledWith(450.5, 320.0, expect.any(Number), 0, Math.PI * 2)
  })

  it('zeichnet tote Spieler (alive=false) nicht', async () => {
    const store = useGameStore()
    const wrapper = mount(GameCanvas)

    store.gameState = {
      tick: 1,
      players: [{ id: 'p1', x: 100, y: 200, hp: 0, score: 0, alive: false, respawnIn: 3, invincible: false, activePowerUp: null, lastProcessedInput: 0 }],
      projectiles: [],
      enemies: [],
      powerUps: [],
      wave: 1,
      enemiesRemaining: 0,
    }
    await wrapper.vm.$nextTick()

    expect(mockCtx.arc).not.toHaveBeenCalledWith(100, 200, expect.any(Number), 0, Math.PI * 2)
  })

  it('zeichnet Projektile an ihrer x/y Position', async () => {
    const store = useGameStore()
    const wrapper = mount(GameCanvas)

    store.gameState = {
      tick: 1,
      players: [],
      projectiles: [{ id: 'b001', x: 500.0, y: 300.0, vx: 0, vy: -10, owner: 'p1' }],
      enemies: [],
      powerUps: [],
      wave: 1,
      enemiesRemaining: 0,
    }
    await wrapper.vm.$nextTick()

    expect(mockCtx.arc).toHaveBeenCalledWith(500.0, 300.0, expect.any(Number), 0, Math.PI * 2)
  })

  it('zeichnet Gegner an ihrer x/y Position', async () => {
    const store = useGameStore()
    const wrapper = mount(GameCanvas)

    store.gameState = {
      tick: 1,
      players: [],
      projectiles: [],
      enemies: [{ id: 'e001', type: 'SCOUT', x: 900.0, y: 50.0, hp: 1 }],
      powerUps: [],
      wave: 1,
      enemiesRemaining: 1,
    }
    await wrapper.vm.$nextTick()

    expect(mockCtx.arc).toHaveBeenCalledWith(900.0, 50.0, expect.any(Number), 0, Math.PI * 2)
  })

  it('verwendet Spielerfarbe aus gameStarting beim Zeichnen', async () => {
    const store = useGameStore()
    const wrapper = mount(GameCanvas)

    store.gameStarting = {
      countdown: 0,
      gameMode: 'COOP',
      fieldWidth: 1920,
      fieldHeight: 1080,
      players: [{ id: 'p1', name: 'Alice', color: '#FF4444', spawnX: 200, spawnY: 540 }],
    }
    store.gameState = {
      tick: 1,
      players: [{ id: 'p1', x: 200, y: 300, hp: 3, score: 0, alive: true, respawnIn: 0, invincible: false, activePowerUp: null, lastProcessedInput: 0 }],
      projectiles: [],
      enemies: [],
      powerUps: [],
      wave: 1,
      enemiesRemaining: 0,
    }
    await wrapper.vm.$nextTick()

    expect(mockCtx._fillStyleValues).toContain('#FF4444')
  })

  it('zeichnet nichts wenn kein gameState vorhanden ist', () => {
    mount(GameCanvas)
    expect(mockCtx.arc).not.toHaveBeenCalled()
    expect(mockCtx.clearRect).not.toHaveBeenCalled()
  })
})
