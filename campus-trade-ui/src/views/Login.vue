<template>
  <div class="login-page">
    <div class="logo">校园二手交易</div>
    <div class="form">
      <input v-model="form.username" placeholder="用户名" />
      <input v-model="form.password" type="password" placeholder="密码" />
      <button @click="handleLogin">登录</button>
      <p class="link" @click="$router.push('/register')">没有账号？去注册</p>
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
.login-page { display: flex; flex-direction: column; align-items: center; padding-top: 80px; min-height: 100vh; background: #f5f5f5; }
.logo { font-size: 24px; font-weight: bold; color: #07c160; margin-bottom: 40px; }
.form { width: 300px; display: flex; flex-direction: column; gap: 15px; }
.form input { padding: 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 15px; outline: none; }
.form button { padding: 12px; background: #07c160; color: #fff; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; }
.link { text-align: center; color: #07c160; font-size: 13px; cursor: pointer; }
</style>
