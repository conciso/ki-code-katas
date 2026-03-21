import { createRouter, createWebHistory } from 'vue-router'
import LobbyCreate from '@/components/organisms/LobbyCreate.vue'
import LobbyJoin from '@/components/organisms/LobbyJoin.vue'
import LobbyView from '@/components/organisms/LobbyView.vue'
import GameView from '@/components/templates/GameView.vue'

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
