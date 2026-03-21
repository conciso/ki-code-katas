<template>
  <div v-if="store.gameState" class="hud">
    <span class="hud-item">Welle {{ store.gameState.wave }}</span>
    <span class="hud-item">Gegner {{ store.gameState.enemiesRemaining }}</span>
    <template v-if="ownPlayer">
      <span class="hud-item">HP {{ ownPlayer.hp }}</span>
      <span class="hud-item">Score {{ ownPlayer.score }}</span>
    </template>
    <span v-if="store.latency !== null" class="hud-item">{{ store.latency }} ms</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useGameStore } from '@/stores/useGameStore'

const store = useGameStore()
const ownPlayer = computed(() =>
  store.gameState?.players.find((p) => p.id === store.playerId) ?? null,
)
</script>
