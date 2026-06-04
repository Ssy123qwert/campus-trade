<template>
  <div class="my-products-page">
    <header class="header">我的发布</header>
    <div v-if="!user" class="login-tip">
      <p>请先登录</p>
      <button @click="$router.push('/login')">去登录</button>
    </div>
    <div v-else-if="products.length === 0" class="empty">暂无发布</div>
    <div v-else class="product-list">
      <div v-for="item in products" :key="item.id" class="product-card">
        <div class="card-header">
          <span class="title">{{ item.title }}</span>
          <span class="status" :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
        </div>
        <p>价格：&yen;{{ item.price }}</p>
        <p>浏览：{{ item.viewCount }} 次</p>
        <div class="actions">
          <button v-if="item.status === 1" @click="offline(item.id)">下架</button>
          <button v-if="item.status === 3" @click="relist(item.id)">重新上架</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { api } from '../api'

export default {
  name: 'MyProducts',
  setup() {
    const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
    const products = ref([])

    const loadProducts = async () => {
      if (!user.value) return
      const res = await api.getMyProducts(user.value.id)
      if (res.code === 200) products.value = res.data
    }

    const statusText = (s) => ({ 1: '在售', 2: '已售', 3: '已下架' }[s] || '')
    const statusClass = (s) => ({ 1: 'on', 2: 'sold', 3: 'off' }[s] || '')

    const offline = async (id) => {
      await api.offlineProduct(id)
      loadProducts()
    }

    const relist = async (id) => {
      await api.updateProduct({ id, status: 1 })
      loadProducts()
    }

    onMounted(loadProducts)

    return { user, products, statusText, statusClass, offline, relist }
  }
}
</script>

<style scoped>
.header { padding: 15px; background: #07c160; color: #fff; text-align: center; font-size: 17px; font-weight: bold; }
.empty { text-align: center; padding: 60px; color: #999; }
.product-list { padding: 10px; }
.product-card { background: #fff; border-radius: 10px; padding: 12px; margin-bottom: 10px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.card-header { display: flex; justify-content: space-between; margin-bottom: 6px; }
.title { font-size: 15px; font-weight: bold; }
.status { font-size: 12px; padding: 2px 8px; border-radius: 10px; }
.status.on { background: #e8f5e9; color: #07c160; }
.status.sold { background: #fff3e0; color: #e6a23c; }
.status.off { background: #f5f5f5; color: #999; }
.product-card p { font-size: 13px; color: #666; margin: 3px 0; }
.actions { margin-top: 8px; }
.actions button { padding: 5px 12px; border: 1px solid #ddd; background: #fff; border-radius: 6px; font-size: 12px; cursor: pointer; margin-right: 8px; }
.login-tip { text-align: center; padding: 80px 20px; }
.login-tip button { margin-top: 15px; padding: 10px 30px; background: #07c160; color: #fff; border: none; border-radius: 8px; font-size: 15px; cursor: pointer; }
</style>
