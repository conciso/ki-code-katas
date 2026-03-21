<template>
  <div class="game-wrapper">
    <GameHUD />
    <GameCanvas class="game-canvas" />
  </div>
</template>

<script setup lang="ts">
import { useKeyboardInput } from '@/composables/useKeyboardInput'
import GameCanvas from './GameCanvas.vue'
import GameHUD from './GameHUD.vue'

useKeyboardInput()
</script>

<style scoped>
.game-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.game-canvas {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
