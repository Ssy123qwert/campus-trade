<template>
  <div class="offers-page">
    <header class="header">
      <span class="back" @click="$router.back()">← 返回</span>
      <span>我的出价</span>
      <span style="width:40px"></span>
    </header>
    <div class="tabs">
      <span :class="{ active: tab === 'my' }" @click="tab = 'my'; load()">我出的价</span>
      <span :class="{ active: tab === 'received' }" @click="tab = 'received'; load()">收到的出价</span>
    </div>
    <div v-if="list.length === 0" class="empty">暂无出价记录</div>
    <div v-else class="offer-list">
      <div v-for="item in list" :key="item.id" class="offer-card">
        <div class="offer-header">
          <span class="product-title">{{ item.product_title }}</span>
          <span :class="['offer-status', statusClass(item.status)]">{{ statusText(item.status) }}</span>
        </div>
        <div class="offer-body">
          <div class="offer-row">出价：<span class="price">&yen;{{ item.price }}</span></div>
          <div class="offer-row" v-if="item.reply_price">卖家还价：<span class="price">&yen;{{ item.reply_price }}</span></div>
          <div class="offer-time">{{ item.create_time }}</div>
        </div>
        <div class="offer-actions" v-if="tab === 'received' && item.status === 0">
          <button class="accept-btn" @click="accept(item.id)">接受</button>
          <button class="reject-btn" @click="reject(item.id)">拒绝</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { api } from '../api'

export default {
  name: 'MyOffers',
  setup() {
    const tab = ref('my')
    const list = ref([])

    const load = async () => {
      const res = tab.value === 'my' ? await api.getMyOffers() : await api.getReceivedOffers()
      if (res.code === 200) list.value = res.data
    }

    const accept = async (id) => {
      await api.acceptOffer(id)
      load()
    }

    const reject = async (id) => {
      await api.rejectOffer(id)
      load()
    }

    const statusText = (s) => ({ 0: '待回复', 1: '已接受', 2: '已拒绝', 3: '已还价' })[s] || ''
    const statusClass = (s) => ({ 0: 'pending', 1: 'accepted', 2: 'rejected' })[s] || ''

    onMounted(load)
    return { tab, list, load, accept, reject, statusText, statusClass }
  }
}
</script>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between; padding: 12px 15px; background: #fff; border-bottom: 1px solid #eee; font-size: 16px; }
.back { color: #07c160; cursor: pointer; }
.tabs { display: flex; background: #fff; border-bottom: 1px solid #eee; }
.tabs span { flex: 1; text-align: center; padding: 12px; font-size: 14px; cursor: pointer; }
.tabs span.active { color: #07c160; border-bottom: 2px solid #07c160; font-weight: bold; }
.empty { text-align: center; padding: 60px; color: #999; }
.offer-list { padding: 10px; }
.offer-card { background: #fff; border-radius: 10px; padding: 12px; margin-bottom: 10px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.offer-header { display: flex; justify-content: space-between; margin-bottom: 8px; }
.product-title { font-size: 14px; font-weight: 600; color: #333; }
.offer-status { font-size: 12px; padding: 2px 8px; border-radius: 4px; }
.offer-status.pending { background: #fff3e0; color: #e6a23c; }
.offer-status.accepted { background: #e8f5e9; color: #07c160; }
.offer-status.rejected { background: #ffebee; color: #f44; }
.offer-row { font-size: 13px; color: #666; margin-bottom: 4px; }
.price { color: #f44; font-weight: bold; }
.offer-time { font-size: 11px; color: #bbb; margin-top: 6px; }
.offer-actions { margin-top: 8px; display: flex; gap: 8px; }
.accept-btn { padding: 6px 16px; background: #07c160; color: #fff; border: none; border-radius: 6px; font-size: 13px; cursor: pointer; }
.reject-btn { padding: 6px 16px; background: #fff; color: #f44; border: 1px solid #f44; border-radius: 6px; font-size: 13px; cursor: pointer; }
</style>
