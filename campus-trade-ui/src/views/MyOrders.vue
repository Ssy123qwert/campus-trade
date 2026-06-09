<template>
  <div class="orders-page">
    <header class="header">我的订单</header>
    <div class="tabs">
      <span :class="{ active: tab === 'buyer' }" @click="tab = 'buyer'; loadOrders()">我买的</span>
      <span :class="{ active: tab === 'seller' }" @click="tab = 'seller'; loadOrders()">我卖的</span>
    </div>
    <div v-if="!user" class="login-tip">
      <p>请先登录</p>
      <button @click="$router.push('/login')">去登录</button>
    </div>
    <div v-else-if="orders.length === 0" class="empty">暂无订单</div>
    <div v-else class="order-list">
      <div v-for="order in orders" :key="order.id" class="order-card">
        <div class="order-header">
          <span>订单号：{{ order.id }}</span>
          <span class="status" :class="statusClass(order.status)">{{ statusText(order.status) }}</span>
        </div>
        <div class="order-body">
          <p>商品ID：{{ order.productId }}</p>
          <p>金额：<span class="price">&yen;{{ order.amount }}</span></p>
          <p>时间：{{ order.createTime }}</p>
        </div>
        <div class="order-actions" v-if="tab === 'buyer' && order.status === 0">
          <button class="pay-now-btn" @click="goPay(order.id)">去支付</button>
        </div>
        <div class="order-actions" v-if="tab === 'seller' && order.status === 1">
          <button @click="ship(order.id)">确认发货</button>
        </div>
        <div class="order-actions" v-else-if="tab === 'buyer' && order.status === 2">
          <button @click="confirm(order.id)">确认收货</button>
        </div>
        <div class="order-actions" v-else-if="order.status === 3">
          <button v-if="tab === 'buyer'" class="review-btn" @click="$router.push('/review/' + order.id)">写评价</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

export default {
  name: 'MyOrders',
  setup() {
    const router = useRouter()
    const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
    const tab = ref('buyer')
    const orders = ref([])

    const loadOrders = async () => {
      if (!user.value) return
      const res = await api.getMyOrders(user.value.id, tab.value)
      if (res.code === 200) orders.value = res.data
    }

    const statusText = (s) => {
      const map = { 0: '待支付', 1: '待发货', 2: '已发货', 3: '已完成', 4: '已取消' }
      return map[s] || ''
    }

    const statusClass = (s) => {
      if (s === 0) return 'status-pay'
      if (s === 1 || s === 2) return 'status'
      if (s === 3) return 'status-done'
      return ''
    }

    const goPay = (orderId) => {
      router.push(`/detail/${order.productId}`)
    }

    const ship = async (id) => {
      await api.updateOrderStatus(id, 2, user.value.id)
      loadOrders()
    }

    const confirm = async (id) => {
      await api.updateOrderStatus(id, 3, user.value.id)
      loadOrders()
    }

    onMounted(loadOrders)

    return { user, tab, orders, statusText, statusClass, goPay, ship, confirm, loadOrders }
  }
}
</script>

<style scoped>
.header { padding: 15px; background: #07c160; color: #fff; text-align: center; font-size: 17px; font-weight: bold; }
.tabs { display: flex; background: #fff; border-bottom: 1px solid #eee; }
.tabs span { flex: 1; text-align: center; padding: 12px; font-size: 14px; cursor: pointer; }
.tabs span.active { color: #07c160; border-bottom: 2px solid #07c160; }
.empty { text-align: center; padding: 60px; color: #999; }
.order-list { padding: 10px; }
.order-card { background: #fff; border-radius: 10px; padding: 12px; margin-bottom: 10px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.order-header { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 8px; }
.status { color: #e6a23c; }
.status-pay { color: #f44; }
.status-done { color: #07c160; }
.order-body { font-size: 13px; color: #666; line-height: 1.8; }
.price { color: #f44; font-weight: bold; }
.order-actions { margin-top: 8px; text-align: right; }
.order-actions button { padding: 6px 15px; background: #07c160; color: #fff; border: none; border-radius: 6px; font-size: 13px; cursor: pointer; margin-left: 8px; }
.order-actions .pay-now-btn { background: #f44; }
.login-tip { text-align: center; padding: 80px 20px; }
.login-tip button { margin-top: 15px; padding: 10px 30px; background: #07c160; color: #fff; border: none; border-radius: 8px; font-size: 15px; cursor: pointer; }
</style>
