<template>
  <div class="review-page">
    <header class="header">
      <button class="back" @click="$router.back()">←</button>
      评价订单
    </header>
    <div class="rating-area">
      <p class="label">评分</p>
      <div class="stars">
        <span v-for="s in 5" :key="s" class="star" :class="{ on: s <= rating }" @click="rating = s">★</span>
      </div>
      <p class="hint">{{ ['', '很差', '较差', '一般', '较好', '非常好'][rating] }}</p>
    </div>
    <div class="content-area">
      <p class="label">评价内容（选填）</p>
      <textarea v-model="content" placeholder="说点什么吧..." maxlength="500"></textarea>
      <p class="counter">{{ content.length }}/500</p>
    </div>
    <button class="submit-btn" :disabled="submitting" @click="submit">
      {{ submitting ? '提交中...' : '提交评价' }}
    </button>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'

export default {
  name: 'CreateReview',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const orderId = route.params.orderId
    const rating = ref(5)
    const content = ref('')
    const submitting = ref(false)

    const submit = async () => {
      submitting.value = true
      const res = await api.createReview({ orderId, rating: rating.value, content: content.value })
      if (res.code === 200) {
        alert('评价成功！')
        router.push('/orders')
      } else {
        alert(res.msg || '评价失败')
      }
      submitting.value = false
    }

    return { orderId, rating, content, submitting, submit }
  }
}
</script>

<style scoped>
.review-page { padding: 16px; max-width: 600px; margin: 0 auto; }
.header { font-size: 18px; font-weight: bold; margin-bottom: 20px; display: flex; align-items: center; gap: 12px; }
.back { background: none; border: none; font-size: 20px; cursor: pointer; }
.label { font-size: 14px; color: #666; margin-bottom: 8px; }
.rating-area { text-align: center; padding: 30px 0; }
.stars { display: flex; justify-content: center; gap: 12px; }
.star { font-size: 40px; color: #ddd; cursor: pointer; transition: color 0.2s; }
.star.on { color: #f59e0b; }
.hint { color: #999; font-size: 13px; margin-top: 8px; }
.content-area { margin: 20px 0; }
textarea { width: 100%; height: 120px; border: 1px solid #ddd; border-radius: 8px; padding: 12px; font-size: 14px; resize: none; outline: none; }
textarea:focus { border-color: #07c160; }
.counter { text-align: right; font-size: 12px; color: #999; margin-top: 4px; }
.submit-btn { width: 100%; padding: 14px; background: #07c160; color: #fff; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; margin-top: 20px; }
.submit-btn:disabled { opacity: 0.6; }
</style>
