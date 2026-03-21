<template>
  <canvas ref="canvasEl" data-testid="game-canvas" />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useGameStore } from '@/stores/useGameStore'

const canvasEl = ref<HTMLCanvasElement | null>(null)
const store = useGameStore()

watch(
  () => store.gameStarting,
  (gs) => {
    if (!gs || !canvasEl.value) return
    canvasEl.value.width = gs.fieldWidth
    canvasEl.value.height = gs.fieldHeight
  },
  { immediate: true },
)

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
      drawShip(ctx, p.x, p.y, colorOf(p.id))
    }

    for (const b of state.projectiles) {
      ctx.fillStyle = colorOf(b.owner)
      ctx.beginPath()
      ctx.arc(b.x, b.y, 3, 0, Math.PI * 2)
      ctx.fill()
    }

    for (const e of state.enemies) {
      drawEnemy(ctx, e.x, e.y)
    }
  },
)

function drawShip(ctx: CanvasRenderingContext2D, x: number, y: number, color: string) {
  const s = 20
  ctx.save()
  ctx.translate(x, y)

  // Hull
  ctx.fillStyle = color
  ctx.beginPath()
  ctx.moveTo(0, -s)                    // Nose
  ctx.lineTo(s * 0.45, s * 0.1)        // Right shoulder
  ctx.lineTo(s * 0.3, s * 0.5)         // Right wing tip
  ctx.lineTo(0, s * 0.2)               // Tail indent
  ctx.lineTo(-s * 0.3, s * 0.5)        // Left wing tip
  ctx.lineTo(-s * 0.45, s * 0.1)       // Left shoulder
  ctx.closePath()
  ctx.fill()

  // Cockpit
  ctx.fillStyle = 'rgba(180, 240, 255, 0.85)'
  ctx.beginPath()
  ctx.ellipse(0, -s * 0.25, s * 0.12, s * 0.22, 0, 0, Math.PI * 2)
  ctx.fill()

  // Engine glow
  ctx.fillStyle = 'rgba(80, 180, 255, 0.9)'
  ctx.beginPath()
  ctx.arc(0, s * 0.3, s * 0.1, 0, Math.PI * 2)
  ctx.fill()

  ctx.restore()
}

function drawEnemy(ctx: CanvasRenderingContext2D, x: number, y: number) {
  const s = 18
  ctx.save()
  ctx.translate(x, y)
  ctx.rotate(Math.PI) // Enemies face downward

  ctx.fillStyle = '#ff3333'
  ctx.beginPath()
  ctx.moveTo(0, -s)
  ctx.lineTo(s * 0.5, s * 0.3)
  ctx.lineTo(s * 0.25, s * 0.5)
  ctx.lineTo(0, s * 0.2)
  ctx.lineTo(-s * 0.25, s * 0.5)
  ctx.lineTo(-s * 0.5, s * 0.3)
  ctx.closePath()
  ctx.fill()

  // Enemy cockpit
  ctx.fillStyle = 'rgba(255, 100, 100, 0.8)'
  ctx.beginPath()
  ctx.ellipse(0, -s * 0.2, s * 0.1, s * 0.18, 0, 0, Math.PI * 2)
  ctx.fill()

  ctx.restore()
}
</script>
