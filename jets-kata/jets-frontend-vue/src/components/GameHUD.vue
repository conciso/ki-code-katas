<template>
  <div v-if="store.gameState" class="hud">
    <span data-testid="hud-wave" class="hud-item">Welle {{ store.gameState.wave }}</span>
    <span data-testid="hud-enemies" class="hud-item">Gegner {{ store.gameState.enemiesRemaining }}</span>
    <template v-if="ownPlayer">
      <span data-testid="hud-hp" class="hud-item">HP {{ ownPlayer.hp }}</span>
      <span data-testid="hud-score" class="hud-item">Score {{ ownPlayer.score }}</span>
      <span v-if="!ownPlayer.alive && ownPlayer.respawnIn > 0" data-testid="hud-respawn" class="hud-item">
        Respawn {{ ownPlayer.respawnIn }}
      </span>
    </template>
    <span
      v-for="p in store.gameState.players"
      :key="p.id"
      data-testid="hud-player-score"
      class="hud-item"
    >{{ p.score }}</span>
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
