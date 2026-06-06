<template>
  <div class="home">
    <header class="header">
      <h1>校园二手交易</h1>
    </header>

    <!-- 公告栏 -->
    <div class="announcement" v-if="announcement">
      <span class="ann-icon">📢</span>
      <span class="ann-text">{{ announcement.content }}</span>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <input v-model="keyword" placeholder="搜索商品..." @keyup.enter="search" />
      <button @click="search">搜索</button>
    </div>

    <!-- 分类筛选 -->
    <div class="categories">
      <span v-for="cat in categories" :key="cat"
            :class="{ active: activeCategory === cat }"
            @click="filterCategory(cat)">{{ cat }}</span>
    </div>

    <!-- 排序 -->
    <div class="sort-bar">
      <span :class="{ active: sort === 'time_desc' }" @click="sort = 'time_desc'; loadProducts()">最新</span>
      <span :class="{ active: sort === 'price_asc' }" @click="sort = 'price_asc'; loadProducts()">价格↑</span>
      <span :class="{ active: sort === 'price_desc' }" @click="sort = 'price_desc'; loadProducts()">价格↓</span>
    </div>

    <!-- 商品列表 -->
    <div class="product-list">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="products.length === 0" class="empty">暂无商品</div>
      <div v-else class="product-grid">
        <div v-for="item in products" :key="item.id" class="product-card" @click="goDetail(item.id)">
          <div class="product-img">
            <img :src="getFirstImage(item.images)" alt="" />
            <span class="condition">{{ conditionText(item.condition) }}</span>
          </div>
          <div class="product-info">
            <h3>{{ item.title }}</h3>
            <p class="desc">{{ item.description }}</p>
            <div class="price-row">
              <span class="price">&yen;{{ item.price }}</span>
              <span class="original" v-if="item.originalPrice">&yen;{{ item.originalPrice }}</span>
              <span class="views">{{ item.viewCount }}次浏览</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载更多 -->
    <div v-if="hasMore" class="load-more" @click="loadMore">加载更多</div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

export default {
  name: 'Home',
  setup() {
    const router = useRouter()
    const keyword = ref('')
    const categories = ref([])
    const activeCategory = ref('')
    const sort = ref('time_desc')
    const products = ref([])
    const loading = ref(false)
    const page = ref(1)
    const hasMore = ref(true)
    const announcement = ref(null)

    const loadProducts = async (isAppend = false) => {
      loading.value = true
      try {
        if (!isAppend) page.value = 1
        const res = await api.getProducts({
          keyword: keyword.value,
          category: activeCategory.value || null,
          sort: sort.value,
          page: page.value,
          size: 10
        })
        if (res.code === 200) {
          const list = res.data.records || []
          if (isAppend) {
            products.value.push(...list)
          } else {
            products.value = list
          }
          hasMore.value = list.length >= 10
        }
      } catch (e) {
        console.error('加载商品失败', e)
      } finally {
        loading.value = false
      }
    }

    const loadCategories = async () => {
      const res = await api.getCategories()
      if (res.code === 200) categories.value = res.data
    }

    const search = () => { loadProducts() }
    const filterCategory = (cat) => {
      activeCategory.value = activeCategory.value === cat ? '' : cat
      loadProducts()
    }
    const loadMore = () => { page.value++; loadProducts(true) }
    const goDetail = (id) => { router.push(`/detail/${id}`) }

    const getFirstImage = (images) => {
      if (!images) return ''
      try {
        const arr = JSON.parse(images)
        return arr[0] || ''
      } catch { return images.split(',')[0] || '' }
    }

    const conditionText = (c) => {
      const map = { 1: '全新', 2: '几乎全新', 3: '轻微使用', 4: '明显使用' }
      return map[c] || ''
    }

    onMounted(() => {
      loadCategories()
      loadProducts()
    })

    return { keyword, categories, activeCategory, sort, products, loading, hasMore,
             search, filterCategory, loadMore, goDetail, getFirstImage, conditionText }
  }
}
</script>

<style scoped>
.header { padding: 15px; background: linear-gradient(135deg, #07c160, #06ad56); color: #fff; text-align: center; }
.header h1 { font-size: 18px; }

.search-bar { display: flex; padding: 10px 15px; gap: 10px; }
.search-bar input { flex: 1; padding: 8px 12px; border: 1px solid #ddd; border-radius: 20px; font-size: 14px; outline: none; }
.search-bar button { padding: 8px 16px; background: #07c160; color: #fff; border: none; border-radius: 20px; font-size: 14px; cursor: pointer; }

.categories { display: flex; padding: 0 15px 10px; gap: 8px; overflow-x: auto; white-space: nowrap; }
.categories span { padding: 5px 12px; background: #f0f0f0; border-radius: 15px; font-size: 12px; cursor: pointer; flex-shrink: 0; }
.categories span.active { background: #07c160; color: #fff; }

.sort-bar { display: flex; padding: 0 15px 10px; gap: 15px; font-size: 13px; color: #666; }
.sort-bar span { cursor: pointer; }
.sort-bar span.active { color: #07c160; font-weight: bold; }

.product-grid { padding: 0 12px; display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.product-card { background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,0.08); cursor: pointer; }
.product-img { position: relative; height: 150px; background: #eee; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.product-img img { width: 100%; height: 100%; object-fit: cover; }
.condition { position: absolute; top: 5px; left: 5px; background: rgba(0,0,0,0.6); color: #fff; font-size: 10px; padding: 2px 6px; border-radius: 8px; }
.product-info { padding: 8px; }
.product-info h3 { font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.desc { font-size: 11px; color: #999; margin: 4px 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.price-row { display: flex; align-items: center; gap: 6px; }
.price { color: #f44; font-size: 16px; font-weight: bold; }
.original { color: #ccc; font-size: 11px; text-decoration: line-through; }
.views { font-size: 10px; color: #bbb; margin-left: auto; }

.loading, .empty { text-align: center; padding: 40px; color: #999; font-size: 14px; }
.load-more { text-align: center; padding: 15px; color: #07c160; font-size: 14px; cursor: pointer; }
</style>
