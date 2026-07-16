import { createApp } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
import App from './App.vue'

import Home from './views/Home.vue'
import Login from './views/Login.vue'
import Register from './views/Register.vue'
import Publish from './views/Publish.vue'
import Detail from './views/Detail.vue'
import Profile from './views/Profile.vue'
import MyOrders from './views/MyOrders.vue'
import MyProducts from './views/MyProducts.vue'
import AiChat from './views/AiChat.vue'
import Chat from './views/Chat.vue'
import Messages from './views/Messages.vue'
import Admin from './views/Admin.vue'
import UserReviews from './views/UserReviews.vue'
import CreateReview from './views/CreateReview.vue'
import Notifications from './views/Notifications.vue'
import MyOffers from './views/MyOffers.vue'
import Statistics from './views/Statistics.vue'

const routes = [
  { path: '/', component: Home },
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  { path: '/publish', component: Publish },
  { path: '/detail/:id', component: Detail },
  { path: '/profile', component: Profile },
  { path: '/orders', component: MyOrders },
  { path: '/my-products', component: MyProducts },
  { path: '/ai-chat', component: AiChat },
  { path: '/chat', component: Chat },
  { path: '/messages', component: Messages },
  { path: '/admin', component: Admin },
  { path: '/reviews/:userId', component: UserReviews },
  { path: '/review/:orderId', component: CreateReview },
  { path: '/notifications', component: Notifications },
  { path: '/offers', component: MyOffers },
  { path: '/statistics', component: Statistics }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

const app = createApp(App)
app.use(router)
app.mount('#app')
