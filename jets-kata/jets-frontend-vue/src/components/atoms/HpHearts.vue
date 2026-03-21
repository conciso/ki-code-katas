<template>
  <span class="hp-hearts">
    <span v-for="i in hp" :key="i" class="hp-heart">♥</span>
  </span>
</template>

<script setup lang="ts">
defineProps<{ hp: number }>()
</script>

<style scoped>
.hp-hearts {
  color: #ff6b6b;
  letter-spacing: -0.1em;
  font-size: 0.65rem;
}
</style>
