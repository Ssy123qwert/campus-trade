<template>
  <div class="detail-page" v-if="product">
    <header class="header">
      <span class="back" @click="$router.back()">← 返回</span>
      <span>商品详情</span>
      <span style="width:40px"></span>
    </header>

    <div class="images">
      <img v-for="(img, i) in imageList" :key="i" :src="img" alt="" />
      <div class="no-img" v-if="imageList.length === 0">暂无图片</div>
    </div>

    <div class="info">
      <h2>{{ product.title }}</h2>
      <div class="price-row">
        <span class="price">&yen;{{ product.price }}</span>
        <span class="original" v-if="product.originalPrice">&yen;{{ product.originalPrice }}</span>
        <span class="condition">{{ conditionText(product.condition) }}</span>
      </div>
      <div class="meta">
        <span>{{ product.category }}</span>
        <span>{{ product.viewCount }} 次浏览</span>
        <span>{{ product.createTime }}</span>
      </div>
      <div class="desc">
        <h4>商品描述</h4>
        <p>{{ product.description || '暂无描述' }}</p>
      </div>
    </div>

    <div class="actions" v-if="user && user.id !== product.userId">
      <button class="fav-btn" :class="{ favorited: isFav }" @click="toggleFav">
        {{ isFav ? '已收藏' : '收藏' }}
      </button>
      <button class="chat-btn" @click="goChat">联系卖家</button>
      <button class="buy-btn" @click="showPayModal = true">立即购买</button>
    </div>
    <div class="actions" v-else-if="!user">
      <button class="buy-btn" @click="$router.push('/login')">登录后购买</button>
    </div>

    <PayModal :visible="showPayModal" :amount="product.price"
      :productTitle="product.title" :sellerName="sellerName"
      @close="showPayModal = false" @success="onPaySuccess" />
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import PayModal from '../components/PayModal.vue'

export default {
  name: 'Detail',
  components: { PayModal },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const product = ref(null)
    const isFav = ref(false)
    const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
    const showPayModal = ref(false)
    const sellerName = ref('')

    const imageList = computed(() => {
      if (!product.value?.images) return []
      try { return JSON.parse(product.value.images) }
      catch { return product.value.images.split(',').filter(Boolean) }
    })

    const conditionText = (c) => {
      const map = { 1: '全新', 2: '几乎全新', 3: '轻微使用', 4: '明显使用' }
      return map[c] || ''
    }

    const loadDetail = async () => {
      const res = await api.getProductDetail(route.params.id)
      if (res.code === 200) {
        product.value = res.data
        // 获取卖家信息
        if (product.value.userId) {
          const uRes = await api.getUserInfo(product.value.userId)
          if (uRes.code === 200) sellerName.value = uRes.data.nickname || uRes.data.username
        }
      }
    }

    const checkFav = async () => {
      if (!user.value) return
      const res = await api.checkFavorite(user.value.id, route.params.id)
      if (res.code === 200) isFav.value = res.data
    }

    const toggleFav = async () => {
      const res = await api.toggleFavorite(user.value.id, route.params.id)
      if (res.code === 200) isFav.value = res.data
    }

    const goChat = () => {
      router.push(`/chat?otherUserId=${product.value.userId}&productId=${product.value.id}`)
    }

    const onPaySuccess = async () => {
      showPayModal.value = false
      // 创建订单
      const createRes = await api.createOrder(route.params.id, user.value.id)
      if (createRes.code === 200) {
        // 支付订单
        const payRes = await api.payOrder(createRes.data.id, user.value.id)
        if (payRes.code === 200) {
          alert('购买成功！请前往订单页面查看')
          router.push('/orders')
        } else {
          alert(payRes.msg || '支付失败')
        }
      } else {
        alert(createRes.msg || '下单失败')
      }
    }

    onMounted(() => {
      loadDetail()
      checkFav()
    })

    return { product, isFav, user, imageList, conditionText, toggleFav, showPayModal, sellerName, goChat, onPaySuccess }
  }
}
</script>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between; padding: 12px 15px; background: #fff; border-bottom: 1px solid #eee; font-size: 16px; }
.back { color: #07c160; cursor: pointer; }

.images { display: flex; overflow-x: auto; gap: 10px; padding: 10px 15px; background: #fff; }
.images img { width: 200px; height: 200px; object-fit: cover; border-radius: 8px; flex-shrink: 0; }
.no-img { width: 100%; height: 200px; background: #eee; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #999; }

.info { padding: 15px; background: #fff; margin-top: 8px; }
.info h2 { font-size: 18px; margin-bottom: 10px; }
.price-row { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.price { font-size: 22px; color: #f44; font-weight: bold; }
.original { font-size: 13px; color: #ccc; text-decoration: line-through; }
.condition { font-size: 12px; background: #fff3e0; color: #e6a23c; padding: 2px 8px; border-radius: 10px; }
.meta { display: flex; gap: 15px; font-size: 12px; color: #999; margin-bottom: 15px; }
.desc h4 { font-size: 14px; margin-bottom: 8px; }
.desc p { font-size: 14px; color: #666; line-height: 1.6; }

.actions { position: fixed; bottom: 0; left: 50%; transform: translateX(-50%); width: 100%; max-width: 480px; display: flex; gap: 8px; padding: 10px 15px; background: #fff; border-top: 1px solid #eee; }
.fav-btn { flex: 1; padding: 12px 8px; border: 1px solid #07c160; background: #fff; color: #07c160; border-radius: 8px; font-size: 14px; cursor: pointer; }
.fav-btn.favorited { background: #07c160; color: #fff; }
.chat-btn { flex: 1; padding: 12px 8px; border: 1px solid #07c160; background: #07c160; color: #fff; border-radius: 8px; font-size: 14px; cursor: pointer; }
.buy-btn { flex: 1.5; padding: 12px; background: #f44; color: #fff; border: none; border-radius: 8px; font-size: 15px; cursor: pointer; }
</style>
