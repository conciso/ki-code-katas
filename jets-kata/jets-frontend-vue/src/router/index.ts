import { createRouter, createWebHistory } from 'vue-router'
import LobbyCreate from '@/components/LobbyCreate.vue'
import LobbyJoin from '@/components/LobbyJoin.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/create' },
    { path: '/create', name: 'create', component: LobbyCreate },
    { path: '/join', name: 'join', component: LobbyJoin },
  ],
})

export default router
