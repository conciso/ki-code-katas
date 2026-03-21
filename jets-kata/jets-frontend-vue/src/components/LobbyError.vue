<template>
  <div v-if="store.lastError" class="error-box">
      <span class="error-code">{{ store.lastError.code }}</span>
      <span class="error-message">{{ store.lastError.message }}</span>
  </div>
</template>

<script setup lang="ts">
import { useGameStore } from '@/stores/useGameStore'
const store = useGameStore()
</script>

<style scoped>
.error-box {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.75rem 1rem;
  border: 1px solid var(--color-danger);
  border-radius: 6px;
  background: rgba(248, 113, 113, 0.08);
}

.error-code {
  font-family: 'Orbitron', sans-serif;
  font-size: 0.7rem;
  letter-spacing: 0.15em;
  color: var(--color-danger);
}

.error-message {
  font-size: 0.9rem;
  color: var(--color-text);
}
</style>
