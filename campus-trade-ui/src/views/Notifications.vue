<template>
  <div class="notif-page">
    <header class="header">
      <span class="back" @click="$router.back()">← 返回</span>
      <span>消息通知</span>
      <span class="clear" @click="markAllRead" v-if="list.length > 0">全部已读</span>
    </header>
    <div v-if="list.length === 0" class="empty">暂无通知</div>
    <div v-else class="notif-list">
      <div v-for="item in list" :key="item.id"
           :class="['notif-item', { unread: !item.isRead }]" @click="markRead(item)">
        <div class="notif-dot" v-if="!item.isRead"></div>
        <div class="notif-body">
          <div class="notif-title">{{ item.title }}</div>
          <div class="notif-content">{{ item.content }}</div>
          <div class="notif-time">{{ formatTime(item.createTime) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { api } from '../api'

export default {
  name: 'Notifications',
  setup() {
    const list = ref([])

    const load = async () => {
      const res = await api.getNotifList()
      if (res.code === 200) list.value = res.data
    }

    const markRead = async (item) => {
      if (!item.isRead) {
        await api.markNotifRead(item.id)
        item.isRead = 1
      }
    }

    const markAllRead = async () => {
      await api.markAllNotifRead()
      list.value.forEach(i => i.isRead = 1)
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

    onMounted(load)
    return { list, markRead, markAllRead, formatTime }
  }
}
</script>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between; padding: 12px 15px; background: #fff; border-bottom: 1px solid #eee; font-size: 16px; position: sticky; top: 0; z-index: 10; }
.back { color: #07c160; cursor: pointer; }
.clear { color: #07c160; font-size: 13px; cursor: pointer; }
.empty { text-align: center; padding: 80px 0; color: #999; font-size: 14px; }
.notif-list { padding: 10px; }
.notif-item { display: flex; align-items: flex-start; gap: 10px; background: #fff; border-radius: 10px; padding: 12px; margin-bottom: 8px; cursor: pointer; }
.notif-item.unread { background: #f0fff4; }
.notif-dot { width: 8px; height: 8px; background: #07c160; border-radius: 50%; flex-shrink: 0; margin-top: 6px; }
.notif-body { flex: 1; min-width: 0; }
.notif-title { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 4px; }
.notif-content { font-size: 13px; color: #666; line-height: 1.4; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.notif-time { font-size: 11px; color: #bbb; margin-top: 6px; }
</style>
