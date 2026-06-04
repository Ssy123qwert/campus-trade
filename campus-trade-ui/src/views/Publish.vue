<template>
  <div class="publish-page">
    <header class="header">发布商品</header>
    <div class="form" v-if="user">
      <input v-model="form.title" placeholder="商品标题" />
      <textarea v-model="form.description" placeholder="商品描述" rows="4"></textarea>
      <div class="row">
        <input v-model.number="form.price" type="number" placeholder="售价" style="flex:1" />
        <input v-model.number="form.originalPrice" type="number" placeholder="原价（选填）" style="flex:1" />
      </div>
      <select v-model="form.category">
        <option value="">选择分类</option>
        <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
      </select>
      <select v-model.number="form.condition">
        <option :value="1">全新</option>
        <option :value="2">几乎全新</option>
        <option :value="3">轻微使用痕迹</option>
        <option :value="4">明显使用痕迹</option>
      </select>
      <div class="image-section">
        <label class="label">商品图片（最多9张）</label>
        <div class="image-grid">
          <div class="image-item" v-for="(img, i) in previewImages" :key="i">
            <img :src="img" alt="" />
            <span class="remove-btn" @click="removeImage(i)">×</span>
          </div>
          <div class="add-btn" @click="triggerUpload" v-if="previewImages.length < 9 && !uploading">
            <span class="plus">+</span>
            <span class="add-text">添加图片</span>
          </div>
          <div class="uploading-item" v-if="uploading">
            <span>上传中...</span>
          </div>
        </div>
        <input ref="fileInput" type="file" accept="image/*" multiple @change="handleFileChange" style="display:none" />
      </div>
      <button @click="handlePublish" :disabled="uploading">{{ uploading ? '请等待上传完成...' : '发布' }}</button>
    </div>
    <div class="login-tip" v-else>
      <p>请先登录</p>
      <button @click="$router.push('/login')">去登录</button>
    </div>
  </div>
</template>

<script>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

export default {
  name: 'Publish',
  setup() {
    const router = useRouter()
    const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
    const categories = ref([])
    const fileInput = ref(null)
    const uploading = ref(false)
    const previewImages = ref([])
    const uploadedUrls = ref([])

    const form = reactive({
      title: '', description: '', price: null, originalPrice: null,
      category: '', condition: 1, userId: user.value?.id
    })

    const loadCategories = async () => {
      const res = await api.getCategories()
      if (res.code === 200) categories.value = res.data
    }

    const triggerUpload = () => {
      fileInput.value.click()
    }

    const handleFileChange = async (e) => {
      const files = Array.from(e.target.files)
      if (files.length === 0) return
      const remaining = 9 - previewImages.value.length
      const toUpload = files.slice(0, remaining)

      for (const file of toUpload) {
        previewImages.value.push(URL.createObjectURL(file))
      }

      uploading.value = true
      try {
        const formData = new FormData()
        toUpload.forEach(f => formData.append('files', f))
        const res = await api.uploadImages(formData)
        if (res.code === 200) {
          uploadedUrls.value.push(...res.data)
        } else {
          alert('图片上传失败: ' + res.msg)
          previewImages.value.splice(previewImages.value.length - toUpload.length, toUpload.length)
        }
      } catch (err) {
        alert('图片上传失败，请重试')
        previewImages.value.splice(previewImages.value.length - toUpload.length, toUpload.length)
      }
      uploading.value = false
      e.target.value = ''
    }

    const removeImage = (i) => {
      previewImages.value.splice(i, 1)
      uploadedUrls.value.splice(i, 1)
    }

    const handlePublish = async () => {
      if (!form.title || !form.price) { alert('请填写标题和价格'); return }
      if (uploading.value) { alert('请等待图片上传完成'); return }
      const data = { ...form, userId: user.value.id, images: JSON.stringify(uploadedUrls.value) }
      const res = await api.publishProduct(data)
      if (res.code === 200) {
        alert('发布成功')
        router.push('/')
      } else {
        alert(res.msg)
      }
    }

    onMounted(loadCategories)

    return { user, categories, form, fileInput, uploading, previewImages, triggerUpload, handleFileChange, removeImage, handlePublish }
  }
}
</script>

<style scoped>
.header { padding: 15px; background: #07c160; color: #fff; text-align: center; font-size: 17px; font-weight: bold; }
.form { padding: 15px; display: flex; flex-direction: column; gap: 12px; }
.form input, .form textarea, .form select { padding: 10px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; outline: none; }
.form textarea { resize: vertical; }
.row { display: flex; gap: 10px; }
.image-section { display: flex; flex-direction: column; gap: 8px; }
.label { font-size: 14px; color: #333; font-weight: 500; }
.image-grid { display: flex; flex-wrap: wrap; gap: 10px; }
.image-item { width: 90px; height: 90px; position: relative; border-radius: 8px; overflow: hidden; border: 1px solid #eee; }
.image-item img { width: 100%; height: 100%; object-fit: cover; }
.remove-btn { position: absolute; top: 2px; right: 2px; width: 22px; height: 22px; background: rgba(0,0,0,0.6); color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; cursor: pointer; line-height: 1; }
.add-btn { width: 90px; height: 90px; border: 2px dashed #ccc; border-radius: 8px; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; color: #999; gap: 4px; transition: all 0.2s; }
.add-btn:hover { border-color: #07c160; color: #07c160; }
.plus { font-size: 28px; line-height: 1; }
.add-text { font-size: 11px; }
.uploading-item { width: 90px; height: 90px; border-radius: 8px; background: #f0f0f0; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #666; }
.form button { padding: 12px; background: #07c160; color: #fff; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; }
.form button:disabled { background: #aaa; cursor: not-allowed; }
.login-tip { text-align: center; padding: 60px 20px; }
.login-tip button { margin-top: 15px; padding: 10px 30px; background: #07c160; color: #fff; border: none; border-radius: 8px; font-size: 15px; cursor: pointer; }
</style>
