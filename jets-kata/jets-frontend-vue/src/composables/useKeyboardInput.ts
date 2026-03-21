import { onMounted, onUnmounted } from 'vue'
import { useGameStore } from '@/stores/useGameStore'

export function useKeyboardInput() {
  const store = useGameStore()
  const state = { up: false, down: false, left: false, right: false, shoot: false }

  function onKeyDown(e: KeyboardEvent) {
    if (e.code === 'ArrowUp' || e.code === 'KeyW') state.up = true
    else if (e.code === 'ArrowDown' || e.code === 'KeyS') state.down = true
    else if (e.code === 'ArrowLeft' || e.code === 'KeyA') state.left = true
    else if (e.code === 'ArrowRight' || e.code === 'KeyD') state.right = true
    else if (e.code === 'Space') state.shoot = true
    else return
    store.sendInput({ ...state })
  }

  function onKeyUp(e: KeyboardEvent) {
    if (e.code === 'ArrowUp' || e.code === 'KeyW') state.up = false
    else if (e.code === 'ArrowDown' || e.code === 'KeyS') state.down = false
    else if (e.code === 'ArrowLeft' || e.code === 'KeyA') state.left = false
    else if (e.code === 'ArrowRight' || e.code === 'KeyD') state.right = false
    else if (e.code === 'Space') state.shoot = false
    else return
    store.sendInput({ ...state })
  }

  onMounted(() => {
    window.addEventListener('keydown', onKeyDown)
    window.addEventListener('keyup', onKeyUp)
  })

  onUnmounted(() => {
    window.removeEventListener('keydown', onKeyDown)
    window.removeEventListener('keyup', onKeyUp)
  })
}
