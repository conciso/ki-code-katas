<template>
  <div class="panel">
    <h2 class="panel-title">Lobby beitreten</h2>

    <BaseInput
      v-model="name"
      label="Rufzeichen"
      data-testid="player-name"
      placeholder="Dein Name"
      autocomplete="off"
      maxlength="20"
    />

    <BaseInput
      v-model="lobbyCode"
      label="Lobby-Code"
      data-testid="lobby-code"
      placeholder="z.B. A3F9K2"
      autocomplete="off"
      maxlength="6"
    />

    <BaseButton :disabled="!name || !lobbyCode" @click="handleJoin">
      Beitreten
    </BaseButton>

    <ul v-if="store.lobby" class="player-list">
      <li
        v-for="player in store.lobby.players"
        :key="player.id"
        class="player-item"
      >
        <span class="player-color" :style="{ background: player.color }" />
        <span class="player-name">{{ player.name }}</span>
        <span class="player-ready" :class="{ ready: player.ready }">
          {{ player.ready ? '✓ Bereit' : 'Warten…' }}
        </span>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useGameStore } from '@/stores/useGameStore'
import BaseInput from './BaseInput.vue'
import BaseButton from './BaseButton.vue'

const name = ref('')
const lobbyCode = ref('')
const store = useGameStore()

function handleJoin() {
  store.connect(name.value)
  store.joinLobby(lobbyCode.value)
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
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  font-size: 0.9rem;
}

.player-color {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 6px currentColor;
}

.player-name {
  flex: 1;
  color: var(--color-heading);
}

.player-ready {
  font-size: 0.75rem;
  letter-spacing: 0.05em;
  color: var(--color-text-muted);
}

.player-ready.ready {
  color: var(--color-success);
}

.panel-title {
  font-family: 'Orbitron', sans-serif;
  font-size: 0.9rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--color-accent);
  text-align: center;
}
</style>
