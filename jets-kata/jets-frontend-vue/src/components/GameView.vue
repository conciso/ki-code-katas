<template>
  <div class="game-view">
    <GameCanvas class="game-canvas" />
    <GameHUD class="game-hud" />
  </div>
</template>

<script setup lang="ts">
import { useKeyboardInput } from '@/composables/useKeyboardInput'
import GameCanvas from './GameCanvas.vue'
import GameHUD from './GameHUD.vue'

useKeyboardInput()
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
  max-width: 100vw;
  max-height: 100vh;
  aspect-ratio: 16 / 9;
  object-fit: contain;
}

.game-hud {
  position: absolute;
  top: 1rem;
  left: 1rem;
  right: 1rem;
  display: flex;
  gap: 1.5rem;
  pointer-events: none;
}
</style>
