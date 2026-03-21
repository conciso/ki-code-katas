<template>
  <span class="player-color-dot" :style="{ background: color }" />
</template>

<script setup lang="ts">
defineProps<{ color: string }>()
</script>

<style scoped>
.player-color-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
