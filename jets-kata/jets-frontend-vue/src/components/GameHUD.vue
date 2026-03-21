<template>
  <div v-if="store.gameState" class="hud">
    <div class="hud-header">
      <span data-testid="hud-wave" class="hud-wave">Welle {{ store.gameState.wave }}</span>
      <span data-testid="hud-enemies" class="hud-enemies">{{ store.gameState.enemiesRemaining }} Gegner</span>
    </div>

    <div class="hud-players">
      <div
        v-for="p in store.gameState.players"
        :key="p.id"
        data-testid="hud-player-score"
        class="hud-player"
        :class="{ 'hud-player--own': p.id === store.playerId }"
      >
        <span class="hud-dot" :style="{ background: colorOf(p.id) }" />
        <span class="hud-name">{{ nameOf(p.id) }}</span>
        <span data-testid="hud-hp" class="hud-hp">
          <span v-for="i in p.hp" :key="i" class="hud-heart">♥</span>
        </span>
        <span data-testid="hud-score" class="hud-score">{{ p.score }}</span>
        <span v-if="!p.alive && p.respawnIn > 0" data-testid="hud-respawn" class="hud-respawn">
          ↺{{ p.respawnIn }}
        </span>
      </div>
    </div>

    <div v-if="store.latency !== null" class="hud-latency">{{ store.latency }} ms</div>
  </div>
</template>

<script setup lang="ts">
import { useGameStore } from '@/stores/useGameStore'

const store = useGameStore()

function colorOf(id: string) {
  return store.gameStarting?.players.find((p) => p.id === id)?.color
    ?? store.lobby?.players.find((p) => p.id === id)?.color
    ?? '#ffffff'
}

function nameOf(id: string) {
  return store.gameStarting?.players.find((p) => p.id === id)?.name
    ?? store.lobby?.players.find((p) => p.id === id)?.name
    ?? id
}
</script>

<style scoped>
.hud {
  background: rgba(5, 8, 20, 0.82);
  border: 1px solid rgba(79, 195, 247, 0.2);
  border-radius: 8px;
  padding: 0.6rem 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  backdrop-filter: blur(8px);
  min-width: 180px;
  font-size: 0.78rem;
  color: var(--color-text, #cdd9e5);
}

.hud-header {
  display: flex;
  justify-content: space-between;
  border-bottom: 1px solid rgba(79, 195, 247, 0.15);
  padding-bottom: 0.3rem;
  margin-bottom: 0.1rem;
}

.hud-wave {
  font-family: 'Orbitron', sans-serif;
  font-size: 0.7rem;
  letter-spacing: 0.1em;
  color: var(--color-accent, #4fc3f7);
}

.hud-enemies {
  color: #ff6b6b;
  font-size: 0.7rem;
}

.hud-players {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.hud-player {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.15rem 0.25rem;
  border-radius: 4px;
}

.hud-player--own {
  background: rgba(79, 195, 247, 0.1);
}

.hud-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.hud-name {
  flex: 1;
  font-size: 0.75rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hud-hp {
  color: #ff6b6b;
  letter-spacing: -0.1em;
  font-size: 0.65rem;
}

.hud-score {
  font-family: 'Orbitron', sans-serif;
  font-size: 0.65rem;
  color: var(--color-accent, #4fc3f7);
  min-width: 2.5rem;
  text-align: right;
}

.hud-respawn {
  font-size: 0.65rem;
  color: #ffd166;
}

.hud-latency {
  font-size: 0.65rem;
  color: rgba(205, 217, 229, 0.4);
  text-align: right;
  border-top: 1px solid rgba(79, 195, 247, 0.1);
  padding-top: 0.25rem;
  margin-top: 0.1rem;
}
</style>
