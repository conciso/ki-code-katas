<template>
  <canvas ref="canvasEl" data-testid="game-canvas" />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useGameStore } from '@/stores/useGameStore'

const canvasEl = ref<HTMLCanvasElement | null>(null)
const store = useGameStore()

watch(
  () => store.gameState,
  (state) => {
    if (!state || !canvasEl.value) return
    const ctx = canvasEl.value.getContext('2d')
    if (!ctx) return

    ctx.clearRect(0, 0, canvasEl.value.width, canvasEl.value.height)

    const startingPlayers = store.gameStarting?.players ?? []
    const colorOf = (id: string) => startingPlayers.find((p) => p.id === id)?.color ?? '#ffffff'

    for (const p of state.players) {
      if (!p.alive) continue
      ctx.fillStyle = colorOf(p.id)
      ctx.beginPath()
      ctx.arc(p.x, p.y, 12, 0, Math.PI * 2)
      ctx.fill()
    }

    for (const b of state.projectiles) {
      ctx.fillStyle = colorOf(b.owner)
      ctx.beginPath()
      ctx.arc(b.x, b.y, 3, 0, Math.PI * 2)
      ctx.fill()
    }

    for (const e of state.enemies) {
      ctx.fillStyle = '#ff0000'
      ctx.beginPath()
      ctx.arc(e.x, e.y, 14, 0, Math.PI * 2)
      ctx.fill()
    }
  },
)
</script>
