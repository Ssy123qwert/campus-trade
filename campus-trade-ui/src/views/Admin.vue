<template>
  <div class="admin-page">
    <header class="admin-header">
      <span class="back" @click="$router.push('/profile')">← 返回</span>
      <h2>管理后台</h2>
      <span></span>
    </header>

    <!-- Tab 切换 -->
    <div class="admin-tabs">
      <span :class="{ active: tab === 'products' }" @click="tab = 'products'; loadProducts()">商品管理</span>
      <span :class="{ active: tab === 'users' }" @click="tab = 'users'; loadUsers()">用户管理</span>
      <span :class="{ active: tab === 'announcement' }" @click="tab = 'announcement'; loadAnnouncements()">公告管理</span>
      <span class="dashboard-link" @click="$router.push('/statistics')">📊 数据看板</span>
    </div>

    <!-- 商品管理 -->
    <div v-if="tab === 'products'" class="admin-content">
      <div class="search-row">
        <input v-model="productKeyword" placeholder="搜索商品..." @keyup.enter="loadProducts()" />
        <button @click="loadProducts()">搜索</button>
      </div>
      <div class="list">
        <div v-for="item in productList" :key="item.id" class="list-item">
          <div class="item-info">
            <strong>{{ item.title }}</strong>
            <p>¥{{ item.price }} | 状态: {{ statusText(item.status) }} | 浏览: {{ item.viewCount }}</p>
          </div>
          <div class="item-actions">
            <button v-if="item.status === 1" class="btn-offline" @click="toggleStatus(item.id, 2)">下架</button>
            <button v-if="item.status === 2" class="btn-online" @click="toggleStatus(item.id, 1)">上架</button>
            <button class="btn-del" @click="deleteProduct(item.id)">删除</button>
          </div>
        </div>
        <div v-if="productList.length === 0" class="empty">暂无数据</div>
      </div>
    </div>

    <!-- 用户管理 -->
    <div v-if="tab === 'users'" class="admin-content">
      <div class="search-row">
        <input v-model="userKeyword" placeholder="搜索用户名/昵称..." @keyup.enter="loadUsers()" />
        <button @click="loadUsers()">搜索</button>
      </div>
      <div class="list">
        <div v-for="item in userList" :key="item.id" class="list-item">
          <div class="item-info">
            <strong>{{ item.nickname || item.username }}</strong>
            <p>{{ item.username }} | {{ item.school || '未设置学校' }} | {{ item.role === 1 ? '管理员' : '普通用户' }}</p>
          </div>
          <div class="item-actions">
            <button class="btn-del" @click="deleteUser(item.id)">删除</button>
          </div>
        </div>
        <div v-if="userList.length === 0" class="empty">暂无数据</div>
      </div>
    </div>

    <!-- 公告管理 -->
    <div v-if="tab === 'announcement'" class="admin-content">
      <div class="announce-editor">
        <textarea v-model="annContent" placeholder="输入公告内容..." rows="4"></textarea>
        <button class="btn-publish" @click="saveAnnouncement" :disabled="!annContent.trim()">发布新公告</button>
      </div>
      <div class="list" style="margin-top: 15px;">
        <div v-for="item in annList" :key="item.id" class="list-item">
          <div class="item-info" style="flex:1">
            <p style="font-size:13px; color:#333; margin-bottom:4px;">{{ item.content }}</p>
            <span style="font-size:11px; color:#999;">{{ item.createTime }}</span>
          </div>
          <div class="item-actions">
            <button class="btn-del" @click="deleteAnnouncement(item.id)">删除</button>
          </div>
        </div>
        <div v-if="annList.length === 0" class="empty">暂无公告</div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

export default {
  name: 'Admin',
  setup() {
    const router = useRouter()
    const tab = ref('products')
    const productList = ref([])
    const productKeyword = ref('')
    const userList = ref([])
    const userKeyword = ref('')
    const annList = ref([])
    const annContent = ref('')

    const checkAccess = async () => {
      const res = await api.checkAdmin()
      if (res.code !== 200 || !res.data) {
        alert('无管理员权限')
        router.push('/')
      }
    }

    const loadProducts = async () => {
      const res = await api.getAdminProducts({ page: 1, size: 100, keyword: productKeyword.value })
      if (res.code === 200) productList.value = res.data.records || []
    }
    const loadUsers = async () => {
      const res = await api.getAdminUsers({ page: 1, size: 100, keyword: userKeyword.value })
      if (res.code === 200) userList.value = res.data.records || []
    }
    const loadAnnouncements = async () => {
      const res = await api.getAnnouncements()
      if (res.code === 200) annList.value = res.data || []
    }

    const deleteProduct = async (id) => {
      if (!confirm('确认删除？')) return
      const res = await api.deleteAdminProduct(id)
      if (res.code === 200) loadProducts()
      else alert(res.message || '删除失败')
    }
    const toggleStatus = async (id, status) => {
      const res = await api.updateProductStatus(id, status)
      if (res.code === 200) loadProducts()
      else alert(res.message || '操作失败')
    }
    const deleteUser = async (id) => {
      if (!confirm('确认删除该用户？')) return
      const res = await api.deleteAdminUser(id)
      if (res.code === 200) loadUsers()
      else alert(res.message || '删除失败')
    }
    const saveAnnouncement = async () => {
      if (!annContent.value.trim()) return
      const res = await api.saveAnnouncement({ content: annContent.value })
      if (res.code === 200) {
        annContent.value = ''
        loadAnnouncements()
      } else alert(res.message || '发布失败')
    }
    const deleteAnnouncement = async (id) => {
      if (!confirm('确认删除该公告？')) return
      const res = await api.deleteAnnouncement(id)
      if (res.code === 200) loadAnnouncements()
      else alert(res.message || '删除失败')
    }

    const statusText = (s) => ({ 1: '在售', 2: '已售', 3: '下架' }[s] || '未知')

    onMounted(() => {
      checkAccess()
      loadProducts()
    })

    return { tab, productList, productKeyword, userList, userKeyword, annList, annContent,
             loadProducts, loadUsers, loadAnnouncements,
             deleteProduct, toggleStatus, deleteUser, saveAnnouncement, deleteAnnouncement,
             statusText }
  }
}
</script>

<style scoped>
.admin-page { min-height: 100vh; background: #f5f5f5; }
.admin-header { display: flex; align-items: center; padding: 12px 15px; background: #333; color: #fff; }
.admin-header h2 { flex: 1; text-align: center; font-size: 17px; }
.back { font-size: 14px; cursor: pointer; }

.admin-tabs { display: flex; background: #fff; border-bottom: 1px solid #eee; }
.admin-tabs span { flex: 1; text-align: center; padding: 12px 0; font-size: 14px; color: #666; cursor: pointer; }
.admin-tabs span.active { color: #07c160; border-bottom: 2px solid #07c160; font-weight: bold; }

.admin-content { padding: 15px; }
.search-row { display: flex; gap: 8px; margin-bottom: 12px; }
.search-row input { flex: 1; padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; outline: none; }
.search-row button { padding: 8px 16px; background: #07c160; color: #fff; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; }

.list-item { background: #fff; border-radius: 8px; padding: 12px; margin-bottom: 8px; display: flex; align-items: center; justify-content: space-between; }
.item-info strong { font-size: 14px; display: block; margin-bottom: 4px; }
.item-info p { font-size: 12px; color: #999; }
.item-actions { display: flex; gap: 6px; flex-shrink: 0; }
.item-actions button { padding: 4px 10px; border: none; border-radius: 4px; font-size: 12px; cursor: pointer; }
.btn-offline { background: #ff9800; color: #fff; }
.btn-online { background: #07c160; color: #fff; }
.btn-del { background: #f44; color: #fff; }

.announce-editor { background: #fff; border-radius: 8px; padding: 12px; }
.announce-editor textarea { width: 100%; border: 1px solid #ddd; border-radius: 6px; padding: 10px; font-size: 14px; resize: vertical; outline: none; }
.btn-publish { margin-top: 10px; padding: 8px 20px; background: #07c160; color: #fff; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; }
.btn-publish:disabled { background: #ccc; cursor: not-allowed; }

.empty { text-align: center; padding: 30px; color: #999; font-size: 14px; }
</style>
