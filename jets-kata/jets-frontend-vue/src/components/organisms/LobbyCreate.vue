<template>
  <div class="panel">
    <h2 class="panel-title">Neue Lobby</h2>

    <BaseInput
      v-model="name"
      label="Name"
      id="player-name"
      placeholder="Dein Name"
      autocomplete="off"
      maxlength="20"
    />

    <BaseButton :disabled="!name" @click="handleCreate">
      Lobby erstellen
    </BaseButton>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '@/stores/useGameStore'
import BaseInput from '../atoms/BaseInput.vue'
import BaseButton from '../atoms/BaseButton.vue'

const name = ref('')
const store = useGameStore()
const router = useRouter()

watch(() => store.lobby, (lobby) => {
  if (lobby) router.push('/lobby')
})

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
