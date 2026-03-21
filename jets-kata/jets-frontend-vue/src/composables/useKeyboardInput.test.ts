// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { useGameStore } from '@/stores/useGameStore'
import { useKeyboardInput } from './useKeyboardInput'

function mountWithInput() {
  const wrapper = mount(
    defineComponent({
      setup() {
        useKeyboardInput()
      },
      template: '<div/>',
    }),
  )
  return wrapper
}

describe('useKeyboardInput', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('sendet up: true beim Drücken von ArrowUp', () => {
    const store = useGameStore()
    vi.spyOn(store, 'sendInput')
    const wrapper = mountWithInput()

    window.dispatchEvent(new KeyboardEvent('keydown', { code: 'ArrowUp' }))

    expect(store.sendInput).toHaveBeenCalledWith(expect.objectContaining({ up: true }))
    wrapper.unmount()
  })

  it('sendet up: true beim Drücken von KeyW', () => {
    const store = useGameStore()
    vi.spyOn(store, 'sendInput')
    const wrapper = mountWithInput()

    window.dispatchEvent(new KeyboardEvent('keydown', { code: 'KeyW' }))

    expect(store.sendInput).toHaveBeenCalledWith(expect.objectContaining({ up: true }))
    wrapper.unmount()
  })

  it('sendet down: true beim Drücken von ArrowDown', () => {
    const store = useGameStore()
    vi.spyOn(store, 'sendInput')
    const wrapper = mountWithInput()

    window.dispatchEvent(new KeyboardEvent('keydown', { code: 'ArrowDown' }))

    expect(store.sendInput).toHaveBeenCalledWith(expect.objectContaining({ down: true }))
    wrapper.unmount()
  })

  it('sendet left: true beim Drücken von ArrowLeft', () => {
    const store = useGameStore()
    vi.spyOn(store, 'sendInput')
    const wrapper = mountWithInput()

    window.dispatchEvent(new KeyboardEvent('keydown', { code: 'ArrowLeft' }))

    expect(store.sendInput).toHaveBeenCalledWith(expect.objectContaining({ left: true }))
    wrapper.unmount()
  })

  it('sendet right: true beim Drücken von ArrowRight', () => {
    const store = useGameStore()
    vi.spyOn(store, 'sendInput')
    const wrapper = mountWithInput()

    window.dispatchEvent(new KeyboardEvent('keydown', { code: 'ArrowRight' }))

    expect(store.sendInput).toHaveBeenCalledWith(expect.objectContaining({ right: true }))
    wrapper.unmount()
  })

  it('sendet shoot: true beim Drücken von Space', () => {
    const store = useGameStore()
    vi.spyOn(store, 'sendInput')
    const wrapper = mountWithInput()

    window.dispatchEvent(new KeyboardEvent('keydown', { code: 'Space' }))

    expect(store.sendInput).toHaveBeenCalledWith(expect.objectContaining({ shoot: true }))
    wrapper.unmount()
  })

  it('sendet up: false beim Loslassen von ArrowUp', () => {
    const store = useGameStore()
    vi.spyOn(store, 'sendInput')
    const wrapper = mountWithInput()

    window.dispatchEvent(new KeyboardEvent('keydown', { code: 'ArrowUp' }))
    window.dispatchEvent(new KeyboardEvent('keyup', { code: 'ArrowUp' }))

    expect(store.sendInput).toHaveBeenLastCalledWith(expect.objectContaining({ up: false }))
    wrapper.unmount()
  })

  it('sendet shoot: false beim Loslassen von Space', () => {
    const store = useGameStore()
    vi.spyOn(store, 'sendInput')
    const wrapper = mountWithInput()

    window.dispatchEvent(new KeyboardEvent('keydown', { code: 'Space' }))
    window.dispatchEvent(new KeyboardEvent('keyup', { code: 'Space' }))

    expect(store.sendInput).toHaveBeenLastCalledWith(expect.objectContaining({ shoot: false }))
    wrapper.unmount()
  })

  it('sendet kombinierten Zustand bei mehreren gleichzeitigen Tasten', () => {
    const store = useGameStore()
    vi.spyOn(store, 'sendInput')
    const wrapper = mountWithInput()

    window.dispatchEvent(new KeyboardEvent('keydown', { code: 'ArrowUp' }))
    window.dispatchEvent(new KeyboardEvent('keydown', { code: 'Space' }))

    expect(store.sendInput).toHaveBeenLastCalledWith(
      expect.objectContaining({ up: true, shoot: true }),
    )
    wrapper.unmount()
  })

  it('entfernt Event-Listener beim Unmounten', () => {
    const store = useGameStore()
    vi.spyOn(store, 'sendInput')
    const wrapper = mountWithInput()

    wrapper.unmount()
    window.dispatchEvent(new KeyboardEvent('keydown', { code: 'ArrowUp' }))

    expect(store.sendInput).not.toHaveBeenCalled()
  })
})
