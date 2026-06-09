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
      <span :class="{ active: sort === 'price_asc' }" @click="sort = 'price_asc'; loadProducts()">价格从低到高</span>
      <span :class="{ active: sort === 'price_desc' }" @click="sort = 'price_desc'; loadProducts()">价格从高到低</span>
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

    const loadAnnouncement = async () => {
      try {
        const res = await api.getAnnouncement()
        if (res.code === 200 && res.data) announcement.value = res.data
      } catch (e) { /* 公告加载失败不影响主页 */ }
    }

    onMounted(() => {
      loadCategories()
      loadProducts()
      loadAnnouncement()
    })

    return { keyword, categories, activeCategory, sort, products, loading, hasMore, announcement,
             search, filterCategory, loadMore, goDetail, getFirstImage, conditionText }
  }
}
</script>

<style scoped>
.header { padding: 18px 16px; background: linear-gradient(135deg, #07c160, #06ad56); color: #fff; text-align: center; position: sticky; top: 0; z-index: 50; }
.header h1 { font-size: 20px; font-weight: 600; letter-spacing: 1px; }
.header p { font-size: 12px; opacity: 0.8; margin-top: 4px; }

.announcement { margin: 12px 16px; padding: 12px 16px; background: linear-gradient(135deg, #fffbeb, #fef3c7); border: 1px solid #fde68a; border-radius: 12px; display: flex; align-items: center; gap: 10px; }
.ann-icon { font-size: 18px; flex-shrink: 0; }
.ann-text { font-size: 13px; color: #92400e; line-height: 1.5; }

.search-bar { display: flex; padding: 12px 16px; gap: 10px; }
.search-bar input { flex: 1; padding: 12px 16px; border: 1.5px solid #eee; border-radius: 25px; font-size: 14px; outline: none; background: #f5f5f7; transition: all 0.3s; }
.search-bar input:focus { border-color: #07c160; background: #fff; box-shadow: 0 0 0 3px rgba(7,193,96,0.1); }
.search-bar button { padding: 12px 24px; background: linear-gradient(135deg, #07c160, #06ad56); color: #fff; border: none; border-radius: 25px; font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.2s; }
.search-bar button:active { transform: scale(0.96); }

.categories { display: flex; padding: 0 16px 12px; gap: 8px; overflow-x: auto; white-space: nowrap; scrollbar-width: none; }
.categories::-webkit-scrollbar { display: none; }
.categories span { padding: 6px 16px; background: #fff; border: 1.5px solid #eee; border-radius: 20px; font-size: 12px; cursor: pointer; flex-shrink: 0; transition: all 0.2s; font-weight: 500; }
.categories span:hover { border-color: #07c160; }
.categories span.active { background: linear-gradient(135deg, #07c160, #06ad56); color: #fff; border-color: transparent; }

.sort-bar { display: flex; padding: 0 16px 12px; gap: 12px; font-size: 13px; }
.sort-bar span { cursor: pointer; color: #999; padding: 4px 0; transition: color 0.2s; }
.sort-bar span.active { color: #07c160; font-weight: 600; position: relative; }
.sort-bar span.active::after { content: ''; position: absolute; bottom: -2px; left: 0; width: 100%; height: 2px; background: #07c160; border-radius: 1px; }

.product-grid { padding: 0 12px; display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.product-card { background: #fff; border-radius: 14px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.06); cursor: pointer; transition: all 0.3s; }
.product-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.product-card:active { transform: scale(0.98); }
.product-img { position: relative; height: 160px; background: linear-gradient(135deg, #f5f5f7, #e8e8ec); display: flex; align-items: center; justify-content: center; overflow: hidden; }
.product-img img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s; }
.product-card:hover .product-img img { transform: scale(1.05); }
.condition { position: absolute; top: 8px; left: 8px; background: rgba(0,0,0,0.55); backdrop-filter: blur(4px); color: #fff; font-size: 10px; padding: 3px 8px; border-radius: 8px; font-weight: 500; }
.product-info { padding: 10px 12px 12px; }
.product-info h3 { font-size: 14px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #1a1a1a; }
.desc { font-size: 11px; color: #999; margin: 4px 0 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.price-row { display: flex; align-items: center; gap: 6px; }
.price { color: #f44; font-size: 17px; font-weight: 700; }
.original { color: #ccc; font-size: 11px; text-decoration: line-through; }
.views { font-size: 10px; color: #bbb; margin-left: auto; }

.loading, .empty { text-align: center; padding: 60px 20px; color: #999; font-size: 14px; }
.load-more { text-align: center; padding: 16px; margin: 8px 16px 16px; color: #667eea; font-size: 14px; cursor: pointer; border: 1.5px dashed #ddd; border-radius: 12px; background: #fff; transition: all 0.2s; }
.load-more:hover { border-color: #667eea; background: #f8f7ff; }
</style>
