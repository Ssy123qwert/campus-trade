<template>
  <div class="messages-page">
    <header class="header">
      <span class="back" @click="$router.back()">← 返回</span>
      <span>消息</span>
      <span style="width:40px"></span>
    </header>

    <div v-if="conversations.length === 0" class="empty">暂无消息</div>
    <div v-for="conv in conversations" :key="conv.productId + '_' + conv.otherUserId"
         class="conv-item" @click="goChat(conv)">
      <div class="conv-avatar">{{ conv.otherUserName?.charAt(0) || '?' }}</div>
      <div class="conv-content">
        <div class="conv-top">
          <span class="conv-name">{{ conv.otherUserName }}</span>
          <span class="conv-time">{{ formatTime(conv.lastTime) }}</span>
        </div>
        <div class="conv-bottom">
          <span class="conv-msg">{{ conv.lastMessage }}</span>
          <span class="conv-badge" v-if="conv.unreadCount > 0">{{ conv.unreadCount }}</span>
        </div>
        <div class="conv-product">{{ conv.productTitle }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

export default {
  name: 'Messages',
  setup() {
    const router = useRouter()
    const userId = ref(Number(localStorage.getItem('userId') || '0'))
    const conversations = ref([])
    let timer = null

    const loadConversations = async () => {
      const res = await api.getConversationList(userId.value)
      if (res.code === 200) conversations.value = res.data
    }

    const goChat = (conv) => {
      router.push(`/chat?otherUserId=${conv.otherUserId}&productId=${conv.productId}`)
    }

    const formatTime = (t) => {
      if (!t) return ''
      const d = new Date(t)
      const now = new Date()
      const isToday = d.toDateString() === now.toDateString()
      const h = String(d.getHours()).padStart(2, '0')
      const m = String(d.getMinutes()).padStart(2, '0')
      if (isToday) return `${h}:${m}`
      return `${d.getMonth() + 1}/${d.getDate()}`
    }

    onMounted(() => {
      loadConversations()
      timer = setInterval(loadConversations, 5000)
    })

    onBeforeUnmount(() => {
      if (timer) clearInterval(timer)
    })

    return { conversations, goChat, formatTime }
  }
}
</script>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between; padding: 12px 15px; background: #fff; border-bottom: 1px solid #eee; font-size: 16px; }
.back { color: #07c160; cursor: pointer; }
.empty { text-align: center; padding: 60px 0; color: #999; font-size: 14px; }

.conv-item { display: flex; align-items: center; gap: 12px; padding: 14px 15px; background: #fff; border-bottom: 1px solid #f5f5f5; cursor: pointer; }
.conv-item:active { background: #f9f9f9; }
.conv-avatar { width: 44px; height: 44px; background: #07c160; color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0; }
.conv-content { flex: 1; min-width: 0; }
.conv-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.conv-name { font-size: 15px; color: #333; }
.conv-time { font-size: 11px; color: #bbb; }
.conv-bottom { display: flex; justify-content: space-between; align-items: center; }
.conv-msg { font-size: 13px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.conv-badge { background: #f44; color: #fff; font-size: 10px; padding: 2px 6px; border-radius: 10px; min-width: 18px; text-align: center; }
.conv-product { font-size: 11px; color: #07c160; margin-top: 3px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
