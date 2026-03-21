// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useGameStore } from '@/stores/useGameStore'
import GameView from './GameView.vue'

vi.mock('@/composables/useKeyboardInput', () => ({ useKeyboardInput: vi.fn() }))
vi.mock('../organisms/GameCanvas.vue', () => ({
  default: { template: '<canvas data-testid="game-canvas" />' },
}))
vi.mock('../organisms/GameHUD.vue', () => ({
  default: { template: '<div data-testid="game-hud" />' },
}))

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/create', component: { template: '<div/>' } },
      { path: '/game', component: { template: '<div/>' } },
    ],
  })
}

describe('GameView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders game canvas', () => {
    const wrapper = mount(GameView, { global: { plugins: [makeRouter()] } })
    expect(wrapper.find('[data-testid="game-canvas"]').exists()).toBe(true)
  })

  it('renders game hud', () => {
    const wrapper = mount(GameView, { global: { plugins: [makeRouter()] } })
    expect(wrapper.find('[data-testid="game-hud"]').exists()).toBe(true)
  })

  it('renders exit button', () => {
    const wrapper = mount(GameView, { global: { plugins: [makeRouter()] } })
    expect(wrapper.find('.btn-exit').exists()).toBe(true)
  })

  it('calls leaveLobby and navigates to /create on exit', async () => {
    const router = makeRouter()
    await router.push('/game')
    const store = useGameStore()
    store.leaveLobby = vi.fn()

    const wrapper = mount(GameView, { global: { plugins: [router] } })

    await wrapper.find('.btn-exit').trigger('click')
    await flushPromises()

    expect(store.leaveLobby).toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/create')
  })
})
