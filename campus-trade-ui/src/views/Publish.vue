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

      <!-- 视频上传 -->
      <div class="media-section">
        <label class="label">商品视频（最多1个）</label>
        <div class="video-area">
          <div class="video-item" v-if="previewVideo">
            <video :src="previewVideo" controls></video>
            <span class="remove-btn" @click="removeVideo">×</span>
          </div>
          <div class="add-btn" @click="triggerVideoUpload" v-if="!previewVideo && !videoUploading">
            <span class="plus">+</span>
            <span class="add-text">添加视频</span>
          </div>
          <div class="uploading-item" v-if="videoUploading">
            <span>视频上传中...</span>
          </div>
        </div>
        <input ref="videoInput" type="file" accept="video/*" @change="handleVideoChange" style="display:none" />
      </div>

      <!-- 图片上传 -->
      <div class="media-section">
        <label class="label">商品图片（最多9张）</label>
        <div class="image-grid">
          <div class="image-item" v-for="(img, i) in previewImages" :key="'img-' + i">
            <img :src="img" alt="" />
            <span class="remove-btn" @click="removeImage(i)">×</span>
          </div>
          <div class="add-btn" @click="triggerImageUpload" v-if="previewImages.length < 9 && !imageUploading">
            <span class="plus">+</span>
            <span class="add-text">添加图片</span>
          </div>
          <div class="uploading-item" v-if="imageUploading">
            <span>图片上传中...</span>
          </div>
        </div>
        <input ref="imageInput" type="file" accept="image/*" multiple @change="handleImageChange" style="display:none" />
      </div>

      <button @click="handlePublish" :disabled="videoUploading || imageUploading">
        {{ (videoUploading || imageUploading) ? '请等待上传完成...' : '发布' }}
      </button>
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
    const imageInput = ref(null)
    const videoInput = ref(null)
    const imageUploading = ref(false)
    const videoUploading = ref(false)
    const previewImages = ref([])
    const uploadedUrls = ref([])
    const previewVideo = ref(null)
    const uploadedVideoUrl = ref(null)

    const form = reactive({
      title: '', description: '', price: null, originalPrice: null,
      category: '', condition: 1, userId: user.value?.id
    })

    const loadCategories = async () => {
      const res = await api.getCategories()
      if (res.code === 200) categories.value = res.data
    }

    // 图片上传
    const triggerImageUpload = () => { imageInput.value.click() }

    const handleImageChange = async (e) => {
      const files = Array.from(e.target.files)
      if (files.length === 0) return
      const remaining = 9 - previewImages.value.length
      const toUpload = files.slice(0, remaining)

      for (const file of toUpload) {
        previewImages.value.push(URL.createObjectURL(file))
      }

      imageUploading.value = true
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
      imageUploading.value = false
      e.target.value = ''
    }

    const removeImage = (i) => {
      previewImages.value.splice(i, 1)
      uploadedUrls.value.splice(i, 1)
    }

    // 视频上传
    const triggerVideoUpload = () => { videoInput.value.click() }

    const handleVideoChange = async (e) => {
      const file = e.target.files[0]
      if (!file) return

      // 前端大小检查
      if (file.size > 50 * 1024 * 1024) {
        alert('视频不能超过50MB')
        e.target.value = ''
        return
      }

      previewVideo.value = URL.createObjectURL(file)
      videoUploading.value = true

      try {
        const formData = new FormData()
        formData.append('files', file)
        const res = await api.uploadImages(formData)
        if (res.code === 200) {
          uploadedVideoUrl.value = res.data[0]
        } else {
          alert('视频上传失败: ' + res.msg)
          previewVideo.value = null
        }
      } catch (err) {
        alert('视频上传失败，请重试')
        previewVideo.value = null
      }
      videoUploading.value = false
      e.target.value = ''
    }

    const removeVideo = () => {
      previewVideo.value = null
      uploadedVideoUrl.value = null
    }

    const handlePublish = async () => {
      if (!form.title || !form.price) { alert('请填写标题和价格'); return }
      if (videoUploading.value || imageUploading.value) { alert('请等待上传完成'); return }
      const data = {
        ...form,
        userId: user.value.id,
        images: JSON.stringify(uploadedUrls.value),
        videoUrl: uploadedVideoUrl.value || ''
      }
      const res = await api.publishProduct(data)
      if (res.code === 200) {
        alert('发布成功')
        router.push('/')
      } else {
        alert(res.msg)
      }
    }

    onMounted(loadCategories)

    return {
      user, categories, form,
      imageInput, videoInput,
      imageUploading, videoUploading,
      previewImages, previewVideo,
      triggerImageUpload, triggerVideoUpload,
      handleImageChange, handleVideoChange,
      removeImage, removeVideo,
      handlePublish
    }
  }
}
</script>

<style scoped>
.header { padding: 15px; background: #07c160; color: #fff; text-align: center; font-size: 17px; font-weight: bold; }
.form { padding: 15px; display: flex; flex-direction: column; gap: 12px; }
.form input, .form textarea, .form select { padding: 10px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; outline: none; }
.form textarea { resize: vertical; }
.row { display: flex; gap: 10px; }
.media-section { display: flex; flex-direction: column; gap: 8px; }
.label { font-size: 14px; color: #333; font-weight: 500; }

/* 视频区域 */
.video-area { display: flex; gap: 10px; align-items: flex-start; }
.video-item { width: 180px; position: relative; border-radius: 8px; overflow: hidden; border: 1px solid #eee; }
.video-item video { width: 100%; height: 120px; object-fit: cover; }

/* 图片区域 */
.image-grid { display: flex; flex-wrap: wrap; gap: 10px; }
.image-item { width: 90px; height: 90px; position: relative; border-radius: 8px; overflow: hidden; border: 1px solid #eee; }
.image-item img { width: 100%; height: 100%; object-fit: cover; }

/* 通用 */
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
