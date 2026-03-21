import { createRouter, createWebHistory } from 'vue-router'
import LobbyCreate from '@/components/LobbyCreate.vue'
import LobbyJoin from '@/components/LobbyJoin.vue'
import LobbyView from '@/components/LobbyView.vue'
import GameView from '@/components/GameView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/create' },
    { path: '/create', name: 'create', component: LobbyCreate },
    { path: '/join', name: 'join', component: LobbyJoin },
    { path: '/lobby', name: 'lobby', component: LobbyView },
    { path: '/game', name: 'game', component: GameView },
  ],
})

export default router
