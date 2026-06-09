<template>
  <div class="login-page">
    <div class="logo-area">
      <div class="logo-icon">🛒</div>
      <div class="logo">校园二手交易</div>
      <div class="logo-sub">安全 · 便捷 · 校园专属</div>
    </div>
    <div class="form">
      <input v-model="form.username" placeholder="请输入用户名" />
      <input v-model="form.password" type="password" placeholder="请输入密码" />
      <button @click="handleLogin">登 录</button>
      <p class="link" @click="$router.push('/register')">还没有账号？去注册 →</p>
    </div>
  </div>
</template>

<script>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

export default {
  name: 'Login',
  setup() {
    const router = useRouter()
    const form = reactive({ username: '', password: '' })

    const handleLogin = async () => {
      if (!form.username || !form.password) { alert('请填写完整'); return }
      const res = await api.login({ username: form.username, password: form.password })
      if (res.code === 200) {
        localStorage.setItem('token', res.data.token)
        localStorage.setItem('user', JSON.stringify(res.data.user))
        router.push('/')
      } else {
        alert(res.msg)
      }
    }

    return { form, handleLogin }
  }
}
</script>

<style scoped>
.login-page { display: flex; flex-direction: column; align-items: center; padding-top: 60px; min-height: 100vh; background: linear-gradient(180deg, #f0fff4 0%, #f5f5f7 100%); }
.logo-area { text-align: center; margin-bottom: 40px; }
.logo-icon { width: 72px; height: 72px; background: linear-gradient(135deg, #07c160, #06ad56); border-radius: 22px; display: flex; align-items: center; justify-content: center; font-size: 32px; margin: 0 auto 16px; box-shadow: 0 8px 24px rgba(7,193,96,0.3); }
.logo { font-size: 26px; font-weight: 700; color: #07c160; }
.logo-sub { font-size: 13px; color: #999; margin-top: 6px; }
.form { width: 320px; display: flex; flex-direction: column; gap: 14px; padding: 24px; background: #fff; border-radius: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.06); }
.form input { padding: 14px 16px; border: 1.5px solid #eee; border-radius: 12px; font-size: 15px; outline: none; transition: border-color 0.2s; background: #fafafa; }
.form input:focus { border-color: #07c160; background: #fff; box-shadow: 0 0 0 3px rgba(7,193,96,0.1); }
.form button { padding: 14px; background: linear-gradient(135deg, #07c160, #06ad56); color: #fff; border: none; border-radius: 12px; font-size: 16px; font-weight: 600; cursor: pointer; transition: all 0.2s; letter-spacing: 1px; }
.form button:active { transform: scale(0.98); opacity: 0.9; }
.link { text-align: center; color: #07c160; font-size: 13px; cursor: pointer; margin-top: 4px; }
</style>
