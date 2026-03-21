<script setup lang="ts">
import LobbyCreate from './components/LobbyCreate.vue'
</script>

<template>
  <div class="stars" aria-hidden="true">
    <span v-for="n in 80" :key="n" class="star" />
  </div>

  <main>
    <header class="game-header">
      <h1 class="game-title">✦ JETS ✦</h1>
      <p class="game-subtitle">Multiplayer Space Combat</p>
    </header>

    <LobbyCreate />
  </main>
</template>

<style scoped>
.stars {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.star {
  position: absolute;
  width: 2px;
  height: 2px;
  border-radius: 50%;
  background: white;
  animation: twinkle var(--duration, 3s) ease-in-out infinite var(--delay, 0s);
  opacity: 0;
}

.star:nth-child(4n) { width: 1px; height: 1px; }
.star:nth-child(7n) { width: 3px; height: 3px; background: var(--color-accent); }

@keyframes twinkle {
  0%, 100% { opacity: 0; }
  50% { opacity: var(--brightness, 0.8); }
}

main {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2.5rem;
  width: 100%;
  max-width: 480px;
}

.game-header {
  text-align: center;
}

.game-title {
  font-family: 'Orbitron', sans-serif;
  font-size: clamp(2.5rem, 8vw, 4rem);
  font-weight: 700;
  color: var(--color-accent);
  letter-spacing: 0.25em;
  text-shadow:
    0 0 20px var(--color-accent-glow),
    0 0 60px var(--color-accent-glow);
  line-height: 1;
}

.game-subtitle {
  margin-top: 0.5rem;
  font-size: 0.85rem;
  letter-spacing: 0.3em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}
</style>

<style>
/* Verteile die Sterne zufällig via CSS Custom Properties */
.star:nth-child(1)  { left:  5%; top: 12%; --duration: 2.8s; --delay: 0.1s; --brightness: 0.9; }
.star:nth-child(2)  { left: 18%; top: 73%; --duration: 4.1s; --delay: 0.7s; --brightness: 0.6; }
.star:nth-child(3)  { left: 31%; top: 28%; --duration: 3.5s; --delay: 1.2s; --brightness: 0.8; }
.star:nth-child(4)  { left: 44%; top: 89%; --duration: 2.3s; --delay: 0.4s; --brightness: 1.0; }
.star:nth-child(5)  { left: 57%; top:  7%; --duration: 3.9s; --delay: 1.8s; --brightness: 0.7; }
.star:nth-child(6)  { left: 70%; top: 55%; --duration: 4.4s; --delay: 0.3s; --brightness: 0.5; }
.star:nth-child(7)  { left: 83%; top: 38%; --duration: 2.6s; --delay: 2.1s; --brightness: 0.9; }
.star:nth-child(8)  { left: 92%; top: 81%; --duration: 3.2s; --delay: 0.9s; --brightness: 0.6; }
.star:nth-child(9)  { left: 11%; top: 45%; --duration: 4.7s; --delay: 1.5s; --brightness: 0.8; }
.star:nth-child(10) { left: 25%; top: 62%; --duration: 2.9s; --delay: 0.2s; --brightness: 1.0; }
.star:nth-child(11) { left: 38%; top: 15%; --duration: 3.7s; --delay: 2.4s; --brightness: 0.7; }
.star:nth-child(12) { left: 52%; top: 77%; --duration: 4.2s; --delay: 0.6s; --brightness: 0.5; }
.star:nth-child(13) { left: 65%; top: 32%; --duration: 2.5s; --delay: 1.9s; --brightness: 0.9; }
.star:nth-child(14) { left: 78%; top: 66%; --duration: 3.4s; --delay: 0.8s; --brightness: 0.8; }
.star:nth-child(15) { left: 88%; top: 20%; --duration: 4.6s; --delay: 1.3s; --brightness: 0.6; }
.star:nth-child(16) { left:  3%; top: 85%; --duration: 2.7s; --delay: 2.7s; --brightness: 1.0; }
.star:nth-child(17) { left: 16%; top: 30%; --duration: 3.1s; --delay: 0.5s; --brightness: 0.7; }
.star:nth-child(18) { left: 29%; top: 50%; --duration: 4.3s; --delay: 1.7s; --brightness: 0.8; }
.star:nth-child(19) { left: 42%; top: 95%; --duration: 2.4s; --delay: 0.1s; --brightness: 0.9; }
.star:nth-child(20) { left: 55%; top: 42%; --duration: 3.8s; --delay: 2.2s; --brightness: 0.5; }
.star:nth-child(21) { left: 68%; top:  3%; --duration: 4.0s; --delay: 1.0s; --brightness: 1.0; }
.star:nth-child(22) { left: 81%; top: 58%; --duration: 2.2s; --delay: 1.6s; --brightness: 0.7; }
.star:nth-child(23) { left: 95%; top: 25%; --duration: 3.6s; --delay: 0.4s; --brightness: 0.8; }
.star:nth-child(24) { left:  8%; top: 68%; --duration: 4.8s; --delay: 2.0s; --brightness: 0.6; }
.star:nth-child(25) { left: 22%; top:  9%; --duration: 2.1s; --delay: 0.7s; --brightness: 0.9; }
.star:nth-child(26) { left: 35%; top: 82%; --duration: 3.3s; --delay: 1.4s; --brightness: 1.0; }
.star:nth-child(27) { left: 49%; top: 22%; --duration: 4.5s; --delay: 0.9s; --brightness: 0.7; }
.star:nth-child(28) { left: 62%; top: 70%; --duration: 2.0s; --delay: 2.5s; --brightness: 0.8; }
.star:nth-child(29) { left: 75%; top: 14%; --duration: 3.0s; --delay: 0.3s; --brightness: 0.5; }
.star:nth-child(30) { left: 89%; top: 47%; --duration: 4.9s; --delay: 1.1s; --brightness: 0.9; }
.star:nth-child(31) { left:  1%; top: 35%; --duration: 2.8s; --delay: 2.3s; --brightness: 1.0; }
.star:nth-child(32) { left: 14%; top: 91%; --duration: 3.7s; --delay: 0.6s; --brightness: 0.6; }
.star:nth-child(33) { left: 27%; top: 18%; --duration: 4.1s; --delay: 1.8s; --brightness: 0.8; }
.star:nth-child(34) { left: 40%; top: 60%; --duration: 2.6s; --delay: 0.2s; --brightness: 0.7; }
.star:nth-child(35) { left: 54%; top:  5%; --duration: 3.4s; --delay: 2.8s; --brightness: 0.9; }
.star:nth-child(36) { left: 67%; top: 88%; --duration: 4.2s; --delay: 1.0s; --brightness: 0.5; }
.star:nth-child(37) { left: 80%; top: 40%; --duration: 2.3s; --delay: 0.5s; --brightness: 1.0; }
.star:nth-child(38) { left: 93%; top: 72%; --duration: 3.9s; --delay: 1.5s; --brightness: 0.7; }
.star:nth-child(39) { left:  6%; top: 55%; --duration: 4.6s; --delay: 0.8s; --brightness: 0.8; }
.star:nth-child(40) { left: 20%; top: 23%; --duration: 2.5s; --delay: 2.1s; --brightness: 0.6; }
.star:nth-child(41) { left: 33%; top: 75%; --duration: 3.1s; --delay: 0.4s; --brightness: 0.9; }
.star:nth-child(42) { left: 47%; top: 48%; --duration: 4.4s; --delay: 1.7s; --brightness: 1.0; }
.star:nth-child(43) { left: 60%; top: 16%; --duration: 2.0s; --delay: 0.1s; --brightness: 0.7; }
.star:nth-child(44) { left: 73%; top: 85%; --duration: 3.5s; --delay: 2.6s; --brightness: 0.8; }
.star:nth-child(45) { left: 86%; top:  8%; --duration: 4.0s; --delay: 0.7s; --brightness: 0.5; }
.star:nth-child(46) { left: 97%; top: 63%; --duration: 2.9s; --delay: 1.3s; --brightness: 0.9; }
.star:nth-child(47) { left:  4%; top: 98%; --duration: 3.6s; --delay: 0.3s; --brightness: 1.0; }
.star:nth-child(48) { left: 17%; top: 40%; --duration: 4.7s; --delay: 1.9s; --brightness: 0.6; }
.star:nth-child(49) { left: 30%; top:  1%; --duration: 2.7s; --delay: 0.6s; --brightness: 0.8; }
.star:nth-child(50) { left: 43%; top: 33%; --duration: 3.2s; --delay: 2.4s; --brightness: 0.7; }
.star:nth-child(51) { left: 56%; top: 92%; --duration: 4.3s; --delay: 0.9s; --brightness: 0.9; }
.star:nth-child(52) { left: 69%; top: 26%; --duration: 2.1s; --delay: 1.2s; --brightness: 1.0; }
.star:nth-child(53) { left: 82%; top: 79%; --duration: 3.8s; --delay: 0.5s; --brightness: 0.5; }
.star:nth-child(54) { left: 91%; top: 11%; --duration: 4.5s; --delay: 2.2s; --brightness: 0.7; }
.star:nth-child(55) { left:  9%; top: 52%; --duration: 2.4s; --delay: 0.2s; --brightness: 0.9; }
.star:nth-child(56) { left: 23%; top: 86%; --duration: 3.0s; --delay: 1.6s; --brightness: 0.8; }
.star:nth-child(57) { left: 36%; top: 39%; --duration: 4.8s; --delay: 0.8s; --brightness: 0.6; }
.star:nth-child(58) { left: 50%; top:  0%; --duration: 2.2s; --delay: 2.9s; --brightness: 1.0; }
.star:nth-child(59) { left: 63%; top: 67%; --duration: 3.3s; --delay: 1.4s; --brightness: 0.7; }
.star:nth-child(60) { left: 76%; top: 21%; --duration: 4.1s; --delay: 0.1s; --brightness: 0.8; }
.star:nth-child(61) { left: 90%; top: 54%; --duration: 2.6s; --delay: 1.8s; --brightness: 0.9; }
.star:nth-child(62) { left:  2%; top: 17%; --duration: 3.4s; --delay: 0.4s; --brightness: 0.5; }
.star:nth-child(63) { left: 15%; top: 80%; --duration: 4.6s; --delay: 2.0s; --brightness: 1.0; }
.star:nth-child(64) { left: 28%; top: 43%; --duration: 2.9s; --delay: 0.7s; --brightness: 0.6; }
.star:nth-child(65) { left: 41%; top:  6%; --duration: 3.7s; --delay: 1.5s; --brightness: 0.8; }
.star:nth-child(66) { left: 53%; top: 57%; --duration: 4.2s; --delay: 0.3s; --brightness: 0.7; }
.star:nth-child(67) { left: 66%; top: 94%; --duration: 2.3s; --delay: 2.7s; --brightness: 0.9; }
.star:nth-child(68) { left: 79%; top: 29%; --duration: 3.5s; --delay: 1.0s; --brightness: 1.0; }
.star:nth-child(69) { left: 94%; top: 76%; --duration: 4.9s; --delay: 0.6s; --brightness: 0.5; }
.star:nth-child(70) { left:  7%; top: 34%; --duration: 2.0s; --delay: 1.3s; --brightness: 0.8; }
.star:nth-child(71) { left: 21%; top: 58%; --duration: 3.9s; --delay: 2.5s; --brightness: 0.7; }
.star:nth-child(72) { left: 34%; top: 97%; --duration: 2.7s; --delay: 0.9s; --brightness: 0.9; }
.star:nth-child(73) { left: 48%; top: 13%; --duration: 4.4s; --delay: 1.7s; --brightness: 1.0; }
.star:nth-child(74) { left: 61%; top: 45%; --duration: 2.1s; --delay: 0.2s; --brightness: 0.6; }
.star:nth-child(75) { left: 74%; top:  2%; --duration: 3.2s; --delay: 2.3s; --brightness: 0.8; }
.star:nth-child(76) { left: 87%; top: 69%; --duration: 4.0s; --delay: 0.5s; --brightness: 0.7; }
.star:nth-child(77) { left: 96%; top: 37%; --duration: 2.8s; --delay: 1.1s; --brightness: 0.9; }
.star:nth-child(78) { left:  0%; top: 78%; --duration: 3.6s; --delay: 0.8s; --brightness: 0.5; }
.star:nth-child(79) { left: 13%; top:  4%; --duration: 4.7s; --delay: 2.1s; --brightness: 1.0; }
.star:nth-child(80) { left: 26%; top: 64%; --duration: 2.5s; --delay: 0.4s; --brightness: 0.7; }
</style>
