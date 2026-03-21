<template>
  <button class="btn-primary" v-bind="$attrs">
    <slot />
  </button>
</template>

<script setup lang="ts">
defineOptions({ inheritAttrs: false })
</script>

<style scoped>
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
</style>
