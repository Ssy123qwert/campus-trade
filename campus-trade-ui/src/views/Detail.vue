<template>
  <div class="detail-page" v-if="product">
    <header class="header">
      <span class="back" @click="$router.back()">← 返回</span>
      <span>商品详情</span>
      <span style="width:40px"></span>
    </header>

    <div class="body-wrap">
      <div class="images" v-if="product.videoUrl || imageList.length > 0">
        <video v-if="product.videoUrl" :src="product.videoUrl" controls class="product-video"></video>
        <img v-for="(img, i) in imageList" :key="i" :src="img" alt="" />
      </div>
      <div class="no-img" v-else>暂无图片</div>

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
</div>

  <div class="actions" v-if="user && user.id != product.userId">
      <button class="fav-btn" :class="{ favorited: isFav }" @click="toggleFav">
        {{ isFav ? '已收藏' : '收藏' }}
      </button>
      <button class="chat-btn" @click="goChat">联系卖家</button>
      <button class="offer-btn" @click="showOffer = true">砍价</button>
      <button class="buy-btn" @click="showPayModal = true">立即购买</button>
    </div>
    <div class="actions" v-else-if="!user">
      <button class="buy-btn" @click="$router.push('/login')">登录后购买</button>
    </div>

    <!-- 砍价弹窗 -->
    <div class="modal-overlay" v-if="showOffer" @click.self="showOffer = false">
      <div class="modal-body">
        <div class="modal-title">向卖家出价</div>
        <div class="offer-row">
          <span>当前价格：&yen;{{ product.price }}</span>
        </div>
        <input v-model="offerPrice" class="offer-input" type="number" placeholder="输入你的出价" />
        <button class="pay-btn" :disabled="!offerPrice || offerPrice <= 0" @click="doOffer">提交出价</button>
        <button class="cancel-btn" @click="showOffer = false">取消</button>
      </div>
    </div>

    <!-- 相似商品推荐 -->
    <div class="similar-section" v-if="similarProducts.length > 0">
      <h4 class="section-title">相似商品推荐</h4>
      <div class="similar-list">
        <div v-for="item in similarProducts" :key="item.id" class="similar-item" @click="$router.push('/detail/' + item.id)">
          <div class="similar-img">{{ item.title?.[0] || '?' }}</div>
          <div class="similar-info">
            <div class="similar-title">{{ item.title }}</div>
            <div class="similar-price">&yen;{{ item.price }}</div>
          </div>
        </div>
      </div>
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
    const showOffer = ref(false)
    const offerPrice = ref('')
    const sellerName = ref('')
    const similarProducts = ref([])

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
          const uRes = await api.getPublicUserInfo(product.value.userId)
          if (uRes.code === 200) sellerName.value = uRes.data.nickname || uRes.data.username
        }
      }
    }

    const checkFav = async () => {
      if (!user.value) return
      const res = await api.checkFavorite(route.params.id)
      if (res.code === 200) isFav.value = res.data
    }

    const toggleFav = async () => {
      const res = await api.toggleFavorite(route.params.id)
      if (res.code === 200) isFav.value = res.data
    }

    const goChat = () => {
      router.push(`/chat?otherUserId=${product.value.userId}&productId=${product.value.id}`)
    }

    const doOffer = async () => {
      if (!offerPrice.value || offerPrice.value <= 0) return
      const res = await api.createOffer(product.value.id, offerPrice.value)
      if (res.code === 200) {
        alert('出价成功，等待卖家回复')
        showOffer.value = false
        offerPrice.value = ''
      } else {
        alert(res.msg || '出价失败')
      }
    }

    const loadSimilar = async () => {
      if (!product.value?.id) return
      const res = await api.getSimilar(product.value.id)
      if (res.code === 200) similarProducts.value = res.data
    }

    const onPaySuccess = async () => {
      showPayModal.value = false
      // 创建订单
      const createRes = await api.createOrder(route.params.id)
      if (createRes.code === 200) {
        // 支付订单
        const payRes = await api.payOrder(createRes.data.id)
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
      loadSimilar()
    })

    return { product, isFav, user, imageList, conditionText, toggleFav, showPayModal, showOffer, offerPrice, sellerName, goChat, onPaySuccess, doOffer, similarProducts }
  }
}
</script>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between; padding: 12px 15px; background: #fff; border-bottom: 1px solid #eee; font-size: 16px; position: sticky; top: 0; z-index: 20; }
.back { color: #07c160; cursor: pointer; }

.images { display: flex; overflow-x: auto; gap: 10px; padding: 10px 15px; background: #fff; }
.images img { width: 200px; height: 200px; object-fit: cover; border-radius: 8px; flex-shrink: 0; }
.product-video { width: 200px; height: 200px; object-fit: cover; border-radius: 8px; flex-shrink: 0; }
.no-img { width: calc(100% - 30px); margin: 10px 15px; height: 200px; background: #eee; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #999; }

.info { padding: 15px; background: #fff; margin-top: 8px; }
.info h2 { font-size: 18px; margin-bottom: 10px; }
.price-row { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.price { font-size: 22px; color: #f44; font-weight: bold; }
.original { font-size: 13px; color: #ccc; text-decoration: line-through; }
.condition { font-size: 12px; background: #fff3e0; color: #e6a23c; padding: 2px 8px; border-radius: 10px; }
.meta { display: flex; gap: 15px; font-size: 12px; color: #999; margin-bottom: 15px; }
.desc h4 { font-size: 14px; margin-bottom: 8px; }
.desc p { font-size: 14px; color: #666; line-height: 1.6; }

.actions { position: fixed; bottom: 0; left: 50%; transform: translateX(-50%); width: 100%; max-width: 480px; display: flex; gap: 8px; padding: 10px 15px; background: #fff; border-top: 1px solid #eee; z-index: 50; }
.fav-btn { flex: 1; padding: 12px 8px; border: 1px solid #07c160; background: #fff; color: #07c160; border-radius: 8px; font-size: 14px; cursor: pointer; }
.fav-btn.favorited { background: #07c160; color: #fff; }
.chat-btn { flex: 1; padding: 12px 8px; border: 1px solid #07c160; background: #07c160; color: #fff; border-radius: 8px; font-size: 14px; cursor: pointer; }
.offer-btn { flex: 1; padding: 12px 8px; border: 1px solid #f59e0b; background: #fff; color: #f59e0b; border-radius: 8px; font-size: 14px; cursor: pointer; }
.buy-btn { flex: 1.5; padding: 12px; background: #f44; color: #fff; border: none; border-radius: 8px; font-size: 15px; cursor: pointer; }

/* 砍价弹窗 */
.modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 200; display: flex; align-items: center; justify-content: center; }
.modal-body { background: #fff; border-radius: 16px; width: 85%; max-width: 360px; padding: 24px; text-align: center; }
.modal-title { font-size: 18px; font-weight: bold; margin-bottom: 20px; color: #333; }
.offer-row { font-size: 14px; color: #666; margin-bottom: 15px; }
.offer-input { width: 100%; padding: 14px 16px; border: 1.5px solid #ddd; border-radius: 12px; font-size: 18px; text-align: center; outline: none; box-sizing: border-box; margin-bottom: 15px; }
.offer-input:focus { border-color: #f59e0b; }
.pay-btn { width: 100%; padding: 12px; background: #07c160; color: #fff; border: none; border-radius: 10px; font-size: 16px; cursor: pointer; margin-bottom: 8px; }
.pay-btn:disabled { background: #ccc; }
.cancel-btn { width: 100%; padding: 12px; background: #fff; color: #999; border: 1px solid #ddd; border-radius: 10px; font-size: 14px; cursor: pointer; }

/* 相似商品 */
.similar-section { padding: 16px; margin-top: 8px; background: #fff; }
.section-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; color: #333; }
.similar-list { display: flex; gap: 10px; overflow-x: auto; padding-bottom: 4px; }
.similar-item { flex-shrink: 0; width: 120px; cursor: pointer; }
.similar-img { width: 120px; height: 120px; background: #f0f0f0; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #999; }
.similar-title { font-size: 12px; color: #333; margin-top: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.similar-price { font-size: 14px; color: #f44; font-weight: bold; margin-top: 2px; }

/* 底部安全区 */
@media (max-width: 767px) {
  .detail-page { padding-bottom: 80px; }
}

/* 平板/桌面: 左图右文 + 操作栏居中加宽 */
@media (min-width: 768px) {
  .detail-page { max-width: 900px; margin: 0 auto; }
  .body-wrap { display: flex; gap: 20px; padding: 16px; }
  .images { flex-direction: column; overflow-x: visible; width: 400px; flex-shrink: 0; padding: 0; border-radius: 12px; overflow: hidden; }
  .images img { width: 400px; height: 300px; border-radius: 0; }
  .product-video { width: 400px; height: 300px; border-radius: 0; }
  .no-img { width: 400px; height: 300px; margin: 0; }
  .info { flex: 1; margin-top: 0; border-radius: 12px; padding: 20px; }
  .info h2 { font-size: 22px; }
  .price { font-size: 26px; }
  .actions { position: relative; bottom: auto; left: auto; transform: none; max-width: none; width: auto; margin: 16px 0 0 0; border-radius: 12px; justify-content: center; }
  .fav-btn, .chat-btn, .buy-btn { flex: 0 1 160px; font-size: 16px; padding: 14px 24px; }
}
</style>
