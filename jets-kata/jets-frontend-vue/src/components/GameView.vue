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
</style>
