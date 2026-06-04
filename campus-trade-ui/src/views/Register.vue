<template>
  <div class="register-page">
    <div class="logo">注册账号</div>
    <div class="form">
      <input v-model="form.username" placeholder="用户名" />
      <input v-model="form.password" type="password" placeholder="密码" />
      <input v-model="form.nickname" placeholder="昵称（选填）" />
      <input v-model="form.school" placeholder="学校" />
      <input v-model="form.phone" placeholder="手机号（选填）" />
      <button @click="handleRegister">注册</button>
      <p class="link" @click="$router.push('/login')">已有账号？去登录</p>
    </div>
  </div>
</template>

<script>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

export default {
  name: 'Register',
  setup() {
    const router = useRouter()
    const form = reactive({ username: '', password: '', nickname: '', school: '', phone: '' })

    const handleRegister = async () => {
      if (!form.username || !form.password) { alert('请填写用户名和密码'); return }
      const res = await api.register(form)
      if (res.code === 200) {
        alert('注册成功，请登录')
        router.push('/login')
      } else {
        alert(res.msg)
      }
    }

    return { form, handleRegister }
  }
}
</script>

<style scoped>
.register-page { display: flex; flex-direction: column; align-items: center; padding-top: 60px; min-height: 100vh; background: #f5f5f5; }
.logo { font-size: 22px; font-weight: bold; color: #07c160; margin-bottom: 30px; }
.form { width: 300px; display: flex; flex-direction: column; gap: 12px; }
.form input { padding: 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 15px; outline: none; }
.form button { padding: 12px; background: #07c160; color: #fff; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; }
.link { text-align: center; color: #07c160; font-size: 13px; cursor: pointer; }
</style>
