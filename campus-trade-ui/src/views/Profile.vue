<template>
  <div class="profile-page">
    <div v-if="user">
      <div class="header-bg"></div>
      <div class="user-card">
        <div class="avatar">{{ user.nickname?.[0] || '?' }}</div>
        <div class="user-info">
          <h3>{{ user.nickname || user.username }}</h3>
          <p>{{ user.school || '未设置学校' }}</p>
        </div>
        <div class="rate-badge" v-if="rate >= 0">
          <span class="rate-num">{{ rate }}%</span>
          <span class="rate-label">好评率</span>
        </div>
      </div>
      <!-- 统计数据卡片 -->
      <div class="stats-row" v-if="stats">
        <div class="stat-card">
          <span class="stat-num">{{ stats.productCount }}</span>
          <span class="stat-label">发布</span>
        </div>
        <div class="stat-card">
          <span class="stat-num">{{ stats.soldCount }}</span>
          <span class="stat-label">卖出</span>
        </div>
        <div class="stat-card">
          <span class="stat-num">{{ stats.boughtCount }}</span>
          <span class="stat-label">买入</span>
        </div>
      </div>
      <div class="menu">
        <div class="menu-item" @click="$router.push('/my-products')">
          <span class="mi-icon">📦</span>
          <span>我的发布</span>
          <span class="mi-arrow">›</span>
        </div>
        <div class="menu-item" @click="$router.push('/orders')">
          <span class="mi-icon">📋</span>
          <span>我的订单</span>
          <span class="mi-arrow">›</span>
        </div>
        <div class="menu-item" @click="$router.push('/offers')">
          <span class="mi-icon">💰</span>
          <span>我的出价</span>
          <span class="mi-arrow">›</span>
        </div>
        <div class="menu-item" @click="$router.push('/reviews/' + (user?.id || 0))">
          <span class="mi-icon">⭐</span>
          <span>我的评价</span>
          <span class="mi-arrow">›</span>
        </div>
        <div class="menu-item" @click="$router.push('/messages')">
          <span class="mi-icon">💬</span>
          <span>我的消息</span>
          <span class="badge" v-if="unreadCount > 0">{{ unreadCount }}</span>
          <span class="mi-arrow">›</span>
        </div>
        <div class="menu-item" @click="$router.push('/ai-chat')">
          <span class="mi-icon">🤖</span>
          <span>AI 智能助手</span>
          <span class="mi-arrow">›</span>
        </div>
        <div class="menu-item" v-if="isAdmin" @click="$router.push('/admin')">
          <span class="mi-icon">⚙️</span>
          <span>管理后台</span>
          <span class="mi-arrow">›</span>
        </div>
        <div class="menu-item logout" @click="logout">
          <span class="mi-icon">🚪</span>
          <span>退出登录</span>
          <span class="mi-arrow">›</span>
        </div>
      </div>
    </div>
    <div class="login-tip" v-else>
      <div class="tip-icon">🛒</div>
      <p>登录后查看更多功能</p>
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
    const rate = ref(-1)
    const stats = ref(null)

    const loadRate = async () => {
      if (!user.value) return
      const res = await api.getReviewRate(user.value.id)
      if (res.code === 200 && res.data >= 0) rate.value = res.data
    }

    const loadUnread = async () => {
      const res = await api.getUnreadCount()
      if (res.code === 200) unreadCount.value = res.data
    }

    const loadStats = async () => {
      const res = await api.getProfileStats()
      if (res.code === 200) stats.value = res.data
    }

    const checkAdmin = async () => {
      const res = await api.checkAdmin()
      if (res.code === 200) isAdmin.value = res.data
      // 防抖：如果 API 没返回，也从本地用户信息判断
      if (!isAdmin.value) {
        const u = JSON.parse(localStorage.getItem('user') || 'null')
        if (u && u.role === 1) isAdmin.value = true
      }
    }

    onMounted(() => {
      user.value = JSON.parse(localStorage.getItem('user') || 'null')
      loadUnread()
      loadStats()
      checkAdmin()
      loadRate()
    })

    const logout = () => {
      api.logout().catch(() => {})  // 后端清除 refreshToken
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      user.value = null
      router.push('/')
    }

    return { user, unreadCount, isAdmin, rate, logout, stats }
  }
}
</script>

<style scoped>
.header-bg { height: 140px; background: linear-gradient(135deg, #07c160 0%, #06ad56 100%); border-radius: 0 0 24px 24px; }
.user-card { display: flex; align-items: center; gap: 15px; padding: 0 20px; margin-top: -70px; }
.avatar { width: 65px; height: 65px; border-radius: 50%; background: linear-gradient(135deg, #07c160, #06ad56); display: flex; align-items: center; justify-content: center; font-size: 28px; color: #fff; box-shadow: 0 4px 12px rgba(7,193,96,0.3); border: 3px solid #fff; flex-shrink: 0; }
.user-info { flex: 1; }
.user-info h3 { color: #1a1a1a; font-size: 20px; font-weight: 600; }
.user-info p { color: #999; font-size: 13px; margin-top: 4px; }
.stats-row { display: flex; gap: 10px; padding: 16px 16px 0; }
.stat-card { flex: 1; background: #fff; border-radius: 12px; padding: 14px; text-align: center; box-shadow: 0 2px 6px rgba(0,0,0,0.04); }
.stat-num { display: block; font-size: 22px; font-weight: 700; color: #07c160; }
.stat-label { font-size: 11px; color: #999; margin-top: 4px; display: block; }
.rate-badge { background: #fff; padding: 8px 14px; border-radius: 12px; text-align: center; box-shadow: 0 2px 6px rgba(0,0,0,0.06); }
.rate-num { display: block; font-size: 20px; font-weight: 700; color: #f59e0b; }
.rate-label { font-size: 10px; color: #999; }
.menu { margin: 16px 16px 0; border-radius: 16px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.menu-item { padding: 16px 18px; background: #fff; border-bottom: 1px solid #f5f5f7; font-size: 15px; cursor: pointer; display: flex; align-items: center; gap: 12px; transition: background 0.2s; }
.menu-item:last-child { border-bottom: none; }
.menu-item:active { background: #f5f5f7; }
.mi-icon { font-size: 20px; width: 24px; text-align: center; }
.mi-arrow { margin-left: auto; color: #ccc; font-size: 18px; }
.badge { background: #f44; color: #fff; font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: 600; margin-left: auto; }
.admin-item { color: #07c160; }
.logout { color: #f44; }
.login-tip { text-align: center; padding: 100px 20px; }
.tip-icon { font-size: 60px; margin-bottom: 16px; }
.login-tip p { color: #999; font-size: 15px; }
.login-tip button { margin-top: 20px; padding: 12px 36px; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; border-radius: 12px; font-size: 15px; font-weight: 500; cursor: pointer; transition: all 0.2s; }
.login-tip button:active { transform: scale(0.98); }

/* 桌面: 资料页居中加宽 */
@media (min-width: 768px) {
  .profile-page { max-width: 600px; margin: 0 auto; }
  .header-bg { height: 180px; }
  .user-card { padding: 0 32px; margin-top: -80px; }
  .avatar { width: 80px; height: 80px; font-size: 34px; }
  .user-info h3 { font-size: 24px; }
  .rate-badge { padding: 12px 18px; }
  .rate-num { font-size: 24px; }
  .menu { margin: 20px 20px 0; }
  .menu-item { padding: 18px 20px; font-size: 16px; }
}
</style>
