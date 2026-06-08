<template>
  <div class="reviews-page">
    <header class="header">
      <button class="back" @click="$router.back()">←</button>
      收到的评价
    </header>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="reviews.length === 0" class="empty">暂无评价</div>
    <div v-else class="review-list">
      <div v-for="r in reviews" :key="r.id" class="review-card">
        <div class="stars">
          <span v-for="s in 5" :key="s" class="star" :class="{ on: s <= r.rating }">★</span>
          <span class="rating-text">{{ r.rating }}分</span>
        </div>
        <p class="content" v-if="r.content">{{ r.content }}</p>
        <p class="meta">{{ r.createTime?.substring(0, 10) }}</p>
      </div>
    </div>
    <div class="pagination" v-if="total > size">
      <button :disabled="page <= 1" @click="load(page - 1)">上一页</button>
      <span>{{ page }} / {{ pages }}</span>
      <button :disabled="page >= pages" @click="load(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'

export default {
  name: 'UserReviews',
  setup() {
    const route = useRoute()
    const reviews = ref([])
    const loading = ref(true)
    const page = ref(1)
    const total = ref(0)
    const size = ref(10)
    const pages = ref(0)

    const load = async (p) => {
      loading.value = true
      page.value = p
      const res = await api.getUserReviews(route.params.userId, p)
      if (res.code === 200) {
        reviews.value = res.data.records || []
        total.value = res.data.total || 0
        pages.value = res.data.pages || 0
      }
      loading.value = false
    }

    onMounted(() => load(1))
    return { reviews, loading, page, total, size, pages, load }
  }
}
</script>

<style scoped>
.reviews-page { padding: 16px; max-width: 600px; margin: 0 auto; }
.header { font-size: 18px; font-weight: bold; margin-bottom: 16px; display: flex; align-items: center; gap: 12px; }
.back { background: none; border: none; font-size: 20px; cursor: pointer; }
.review-card { background: #fff; border-radius: 8px; padding: 14px; margin-bottom: 10px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.stars { margin-bottom: 6px; }
.star { color: #ddd; font-size: 18px; }
.star.on { color: #f59e0b; }
.rating-text { font-size: 13px; color: #666; margin-left: 6px; }
.content { color: #333; font-size: 14px; line-height: 1.5; margin: 6px 0; }
.meta { color: #999; font-size: 12px; }
.empty { text-align: center; color: #999; margin-top: 80px; }
.loading { text-align: center; color: #999; margin-top: 80px; }
.pagination { display: flex; justify-content: center; align-items: center; gap: 12px; margin-top: 16px; }
.pagination button { padding: 6px 16px; border: 1px solid #ddd; border-radius: 4px; background: #fff; cursor: pointer; }
.pagination button:disabled { opacity: 0.4; cursor: default; }
</style>
