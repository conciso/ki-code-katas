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

    <LobbyError />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '@/stores/useGameStore'
import BaseInput from './BaseInput.vue'
import BaseButton from './BaseButton.vue'
import LobbyError from './LobbyError.vue'

const name = ref('')
const lobbyCode = ref('')
const store = useGameStore()
const router = useRouter()

watch(() => store.lobby, (lobby) => {
  if (lobby) router.push('/lobby')
})

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


.panel-title {
  font-family: 'Orbitron', sans-serif;
  font-size: 0.9rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--color-accent);
  text-align: center;
}
</style>
