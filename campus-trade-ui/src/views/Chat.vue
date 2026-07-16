<template>
  <div class="chat-page">
    <header class="header">
      <span class="back" @click="$router.back()">← 返回</span>
      <span>{{ otherUser?.nickname || '聊天' }}</span>
      <span style="width:40px"></span>
    </header>

    <div class="product-bar" v-if="product">
      <img :src="productImage" alt="" />
      <div class="product-info">
        <span class="product-title">{{ product.title }}</span>
        <span class="product-price">&yen;{{ product.price }}</span>
      </div>
    </div>

    <div class="message-list" ref="msgList">
      <div v-if="messages.length === 0" class="empty">暂无消息，发送第一条消息吧</div>
      <div v-for="msg in messages" :key="msg.id"
           :class="['message', msg.fromUserId === userId ? 'mine' : 'other']">
        <div class="msg-content">{{ msg.content }}</div>
        <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
      </div>
    </div>

    <div class="input-bar">
      <input v-model="inputText" placeholder="输入消息..." @keyup.enter="sendMsg" />
      <button @click="sendMsg" :disabled="!inputText.trim()">发送</button>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'

export default {
  name: 'Chat',
  setup() {
    const route = useRoute()
    const userStr = localStorage.getItem('user')
    const currentUser = userStr ? JSON.parse(userStr) : null
    const userId = ref(currentUser?.id || 0)
    const otherUserId = ref(Number(route.query.otherUserId || '0'))
    const productId = ref(Number(route.query.productId || '0'))
    const messages = ref([])
    const inputText = ref('')
    const otherUser = ref(null)
    const product = ref(null)
    const msgList = ref(null)
    let timer = null

    const productImage = ref('')

    const loadOtherUser = async () => {
      const res = await api.getPublicUserInfo(otherUserId.value)
      if (res.code === 200) otherUser.value = res.data
    }

    const loadProduct = async () => {
      const res = await api.getProductDetail(productId.value)
      if (res.code === 200) {
        product.value = res.data
        if (product.value.images) {
          try {
            const arr = JSON.parse(product.value.images)
            productImage.value = arr[0] || ''
          } catch { productImage.value = product.value.images.split(',')[0] || '' }
        }
      }
    }

    const loadMessages = async () => {
      const res = await api.getConversation(otherUserId.value, productId.value)
      if (res.code === 200) messages.value = res.data
      await nextTick()
      scrollToBottom()
    }

    const scrollToBottom = () => {
      if (msgList.value) {
        msgList.value.scrollTop = msgList.value.scrollHeight
      }
    }

    const sendMsg = async () => {
      const text = inputText.value.trim()
      if (!text) return
      inputText.value = ''
      const res = await api.sendMessage(otherUserId.value, productId.value, text)
      if (res.code === 200) {
        messages.value.push(res.data)
        await nextTick()
        scrollToBottom()
      }
    }

    const formatTime = (t) => {
      if (!t) return ''
      const d = new Date(t)
      const now = new Date()
      const isToday = d.toDateString() === now.toDateString()
      const h = String(d.getHours()).padStart(2, '0')
      const m = String(d.getMinutes()).padStart(2, '0')
      if (isToday) return `${h}:${m}`
      return `${d.getMonth() + 1}/${d.getDate()} ${h}:${m}`
    }

    onMounted(() => {
      loadOtherUser()
      loadProduct()
      loadMessages()
      // 每3秒轮询新消息
      timer = setInterval(loadMessages, 3000)
    })

    onBeforeUnmount(() => {
      if (timer) clearInterval(timer)
    })

    return { userId, otherUser, otherUserId, product, productImage, messages, inputText, msgList, sendMsg, formatTime }
  }
}
</script>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between; padding: 12px 15px; background: #fff; border-bottom: 1px solid #eee; font-size: 16px; }
.back { color: #07c160; cursor: pointer; }

.product-bar { display: flex; align-items: center; gap: 10px; padding: 10px 15px; background: #f9f9f9; border-bottom: 1px solid #eee; }
.product-bar img { width: 44px; height: 44px; object-fit: cover; border-radius: 6px; }
.product-info { display: flex; flex-direction: column; }
.product-title { font-size: 13px; color: #333; }
.product-price { font-size: 15px; color: #f44; font-weight: bold; }

.message-list { flex: 1; overflow-y: auto; padding: 15px; display: flex; flex-direction: column; gap: 12px; }
.empty { text-align: center; color: #999; font-size: 13px; margin-top: 60px; }

.message { max-width: 75%; }
.message.mine { align-self: flex-end; }
.message.other { align-self: flex-start; }
.msg-content { padding: 10px 14px; border-radius: 16px; font-size: 14px; line-height: 1.5; word-break: break-word; }
.mine .msg-content { background: #07c160; color: #fff; border-bottom-right-radius: 4px; }
.other .msg-content { background: #f0f0f0; color: #333; border-bottom-left-radius: 4px; }
.msg-time { font-size: 10px; color: #bbb; margin-top: 4px; text-align: center; }

.input-bar { display: flex; gap: 10px; padding: 10px 15px; background: #fff; border-top: 1px solid #eee; }
.input-bar input { flex: 1; padding: 10px 14px; border: 1px solid #ddd; border-radius: 20px; font-size: 14px; outline: none; }
.input-bar button { padding: 10px 20px; background: #07c160; color: #fff; border: none; border-radius: 20px; font-size: 14px; cursor: pointer; }
.input-bar button:disabled { background: #ccc; }
</style>
