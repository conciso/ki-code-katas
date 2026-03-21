<template>
  <div class="game-view">
    <GameCanvas class="game-canvas" />
    <GameHUD class="game-hud" />
    <button class="btn-exit" @click="exit">✕ Exit</button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useKeyboardInput } from '@/composables/useKeyboardInput'
import { useGameStore } from '@/stores/useGameStore'
import GameCanvas from './GameCanvas.vue'
import GameHUD from './GameHUD.vue'

const store = useGameStore()
const router = useRouter()

useKeyboardInput()

function exit() {
  store.leaveLobby()
  router.push('/create')
}
</script>

<style scoped>
.game-view {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.game-canvas {
  display: block;
  width: min(100vw, calc(100vh * 16 / 9));
  height: min(100vh, calc(100vw * 9 / 16));
}

.game-hud {
  position: absolute;
  top: 1rem;
  right: 1rem;
  pointer-events: none;
  z-index: 10;
}

.btn-exit {
  position: absolute;
  top: 1rem;
  left: 1rem;
  background: rgba(5, 8, 20, 0.75);
  border: 1px solid rgba(255, 107, 107, 0.4);
  color: #ff6b6b;
  font-size: 0.75rem;
  letter-spacing: 0.1em;
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  cursor: pointer;
  backdrop-filter: blur(8px);
  transition: background 0.15s, border-color 0.15s;
}

.btn-exit:hover {
  background: rgba(255, 107, 107, 0.15);
  border-color: rgba(255, 107, 107, 0.7);
}
</style>
