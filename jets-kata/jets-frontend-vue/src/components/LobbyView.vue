<template>
  <div class="panel">
    <header class="lobby-header">
      <div class="lobby-meta">
        <span class="lobby-code">{{ store.lobby?.lobbyCode }}</span>
        <span class="lobby-mode">{{ store.lobby?.gameMode }}</span>
      </div>
    </header>

    <ul class="player-list">
      <li
        v-for="player in store.lobby?.players"
        :key="player.id"
        class="player-item"
        data-testid="player-item"
      >
        <span
          class="player-color"
          data-testid="player-color"
          :style="{ background: player.color }"
        />
        <span class="player-name">{{ player.name }}</span>
        <span class="player-status" :class="{ ready: player.ready }">
          {{ player.ready ? 'Bereit' : 'Warten' }}
        </span>
      </li>
    </ul>

    <div class="actions">
      <BaseButton data-testid="btn-ready" @click="toggleReady">
        {{ ownPlayer?.ready ? 'Nicht bereit' : 'Bereit' }}
      </BaseButton>

      <BaseButton
        v-if="store.isHost"
        data-testid="btn-start"
        :disabled="!allReady"
        @click="store.startGame()"
      >
        Spiel starten
      </BaseButton>

      <BaseButton
        v-if="store.isHost"
        data-testid="btn-end-session"
        variant="danger"
        @click="store.leaveLobby()"
      >
        Session beenden
      </BaseButton>

      <BaseButton data-testid="btn-leave" @click="store.leaveLobby()">
        Verlassen
      </BaseButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '@/stores/useGameStore'
import BaseButton from './BaseButton.vue'

const store = useGameStore()
const router = useRouter()

watch(() => store.gameStarting, (gs) => {
  if (gs) router.push('/game')
})

watch(() => store.lobby, (lobby) => {
  if (!lobby) router.push('/create')
})

const ownPlayer = computed(() =>
  store.lobby?.players.find((p) => p.id === store.playerId),
)

const allReady = computed(() =>
  !!store.lobby?.players.length && store.lobby.players.every((p) => p.ready),
)

function toggleReady() {
  store.setReady(!ownPlayer.value?.ready)
}
</script>

<style scoped>
.panel {
  width: 100%;
  background: rgba(13, 15, 31, 0.85);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 2rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  backdrop-filter: blur(12px);
  box-shadow:
    0 0 0 1px rgba(79, 195, 247, 0.05),
    0 8px 40px rgba(0, 0, 0, 0.6);
}

.lobby-header {
  display: flex;
  justify-content: center;
}

.lobby-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.lobby-code {
  font-family: 'Orbitron', sans-serif;
  font-size: 1.4rem;
  font-weight: 700;
  letter-spacing: 0.25em;
  color: var(--color-accent);
  text-shadow: 0 0 16px var(--color-accent-glow);
}

.lobby-mode {
  font-size: 0.75rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 0.2rem 0.5rem;
}

.player-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.player-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.6rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
}

.player-color {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.player-name {
  flex: 1;
  color: var(--color-heading);
}

.player-status {
  font-size: 0.75rem;
  letter-spacing: 0.05em;
  color: var(--color-text-muted);
}

.player-status.ready {
  color: var(--color-success);
}

.actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}
</style>
