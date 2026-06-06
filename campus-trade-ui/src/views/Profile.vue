<template>
  <div class="profile-page">
    <div v-if="user">
      <div class="user-card">
        <div class="avatar">{{ user.nickname?.[0] || '?' }}</div>
        <div class="user-info">
          <h3>{{ user.nickname || user.username }}</h3>
          <p>{{ user.school || '未设置学校' }}</p>
        </div>
      </div>
      <div class="menu">
        <div class="menu-item" @click="$router.push('/my-products')">我的发布</div>
        <div class="menu-item" @click="$router.push('/orders')">我的订单</div>
        <div class="menu-item" @click="$router.push('/messages')">
          我的消息
          <span class="badge" v-if="unreadCount > 0">{{ unreadCount }}</span>
        </div>
        <div class="menu-item" @click="$router.push('/ai-chat')">AI 智能助手</div>
        <div class="menu-item admin-item" v-if="isAdmin" @click="$router.push('/admin')">⚙️ 管理后台</div>
        <div class="menu-item logout" @click="logout">退出登录</div>
      </div>
    </div>
    <div class="login-tip" v-else>
      <p>请先登录</p>
      <button @click="$router.push('/login')">去登录</button>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

export default {
  name: 'Profile',
  setup() {
    const router = useRouter()
    const user = ref(null)
    const unreadCount = ref(0)
    const isAdmin = ref(false)

    const loadUnread = async () => {
      if (!user.value) return
      const res = await api.getUnreadCount(user.value.id)
      if (res.code === 200) unreadCount.value = res.data
    }

    const checkAdmin = async () => {
      const res = await api.checkAdmin()
      if (res.code === 200) isAdmin.value = res.data
    }

    onMounted(() => {
      user.value = JSON.parse(localStorage.getItem('user') || 'null')
      loadUnread()
      checkAdmin()
    })

    const logout = () => {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      user.value = null
      router.push('/')
    }

    return { user, unreadCount, isAdmin, logout }
  }
}
</script>

<style scoped>
.user-card { display: flex; align-items: center; gap: 15px; padding: 25px 20px; background: linear-gradient(135deg, #07c160, #06ad56); }
.avatar { width: 55px; height: 55px; border-radius: 50%; background: rgba(255,255,255,0.3); display: flex; align-items: center; justify-content: center; font-size: 24px; color: #fff; }
.user-info h3 { color: #fff; font-size: 18px; }
.user-info p { color: rgba(255,255,255,0.8); font-size: 13px; margin-top: 4px; }
.menu { margin-top: 10px; background: #fff; }
.menu-item { padding: 15px 20px; border-bottom: 1px solid #f5f5f5; font-size: 15px; cursor: pointer; display: flex; align-items: center; justify-content: space-between; }
.badge { background: #f44; color: #fff; font-size: 11px; padding: 2px 7px; border-radius: 10px; }
.menu-item:active { background: #f5f5f5; }
.badge { display: inline-block; background: #f44; color: #fff; font-size: 10px; padding: 2px 6px; border-radius: 10px; margin-left: 8px; vertical-align: middle; }
.admin-item { color: #07c160; font-weight: bold; }
.logout { color: #f44; text-align: center; }
.login-tip { text-align: center; padding: 80px 20px; }
.login-tip button { margin-top: 15px; padding: 10px 30px; background: #07c160; color: #fff; border: none; border-radius: 8px; font-size: 15px; cursor: pointer; }
</style>
