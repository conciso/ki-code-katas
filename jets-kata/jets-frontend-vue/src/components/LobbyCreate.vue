<template>
  <div class="panel">
    <h2 class="panel-title">Neue Lobby</h2>

    <div class="field">
      <label class="field-label" for="player-name">Rufzeichen</label>
      <input
        id="player-name"
        v-model="name"
        class="field-input"
        placeholder="Dein Name"
        autocomplete="off"
        maxlength="20"
      />
    </div>

    <button class="btn-primary" :disabled="!name" @click="handleCreate">
      Lobby erstellen
    </button>

    <div v-if="store.lobby" class="lobby-code">
      <span class="lobby-code-label">Lobby-Code</span>
      <span class="lobby-code-value">{{ store.lobby.lobbyCode }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useGameStore } from '@/stores/useGameStore'

const name = ref('')
const store = useGameStore()

function handleCreate() {
  store.connect(name.value)
  store.createLobby()
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

.panel-title {
  font-family: 'Orbitron', sans-serif;
  font-size: 0.9rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--color-accent);
  text-align: center;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.field-label {
  font-size: 0.75rem;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.field-input {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: var(--color-heading);
  font-family: inherit;
  font-size: 1rem;
  padding: 0.6rem 0.9rem;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.field-input::placeholder {
  color: var(--color-text-muted);
}

.field-input:focus {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px var(--color-accent-glow);
}

.btn-primary {
  padding: 0.75rem 1.5rem;
  background: transparent;
  border: 1px solid var(--color-accent);
  border-radius: 6px;
  color: var(--color-accent);
  font-family: 'Orbitron', sans-serif;
  font-size: 0.8rem;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  cursor: pointer;
  transition: background 0.2s, box-shadow 0.2s;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-accent-glow);
  box-shadow: 0 0 16px var(--color-accent-glow);
}

.btn-primary:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.lobby-code {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
  padding: 1rem;
  border: 1px solid rgba(79, 195, 247, 0.2);
  border-radius: 8px;
  background: rgba(79, 195, 247, 0.05);
}

.lobby-code-label {
  font-size: 0.7rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.lobby-code-value {
  font-family: 'Orbitron', sans-serif;
  font-size: 1.8rem;
  font-weight: 700;
  letter-spacing: 0.3em;
  color: var(--color-accent);
  text-shadow: 0 0 20px var(--color-accent-glow);
}
</style>
