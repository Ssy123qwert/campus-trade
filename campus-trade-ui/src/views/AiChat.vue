<template>
  <div class="ai-page">
    <header class="header">AI 智能助手</header>
    <div class="intro">
      <p>我可以帮你：</p>
      <div class="tips">
        <span @click="ask('帮我推荐一些性价比高的二手书籍')">推荐二手书籍</span>
        <span @click="ask('我想买一个二手手机，有什么建议？')">选购建议</span>
        <span @click="ask('二手商品如何定价比较合理？')">定价建议</span>
        <span @click="ask('校园二手交易要注意什么？')">交易指南</span>
      </div>
    </div>

    <div class="chat-list" ref="chatList">
      <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
        <div class="bubble">{{ msg.content }}</div>
      </div>
      <div v-if="loading" class="msg ai">
        <div class="bubble typing">AI 思考中...</div>
      </div>
    </div>

    <div class="input-bar">
      <input v-model="input" placeholder="问AI任何关于二手交易的问题..." @keyup.enter="send" />
      <button @click="send" :disabled="loading || !input.trim()">发送</button>
    </div>
  </div>
</template>

<script>
import { ref, nextTick, onMounted } from 'vue'

export default {
  name: 'AiChat',
  setup() {
    const messages = ref([])
    const input = ref('')
    const loading = ref(false)
    const chatList = ref(null)

    const scrollBottom = async () => {
      await nextTick()
      if (chatList.value) chatList.value.scrollTop = chatList.value.scrollHeight
    }

    const send = async () => {
      const text = input.value.trim()
      if (!text || loading.value) return
      messages.value.push({ role: 'user', content: text })
      input.value = ''
      loading.value = true
      await scrollBottom()

      try {
        const res = await fetch('/api/ai/chat', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ question: text })
        })
        const data = await res.json()
        if (data.code === 200) {
          messages.value.push({ role: 'ai', content: data.data })
        } else {
          messages.value.push({ role: 'ai', content: '抱歉，AI服务暂时不可用：' + data.msg })
        }
      } catch (e) {
        messages.value.push({ role: 'ai', content: 'AI服务连接失败，请稍后重试。' })
      }
      loading.value = false
      await scrollBottom()
    }

    const ask = (q) => {
      input.value = q
      send()
    }

    return { messages, input, loading, chatList, send, ask }
  }
}
</script>

<style scoped>
.ai-page { display: flex; flex-direction: column; height: calc(100vh - 55px); }
.header { padding: 15px; background: linear-gradient(135deg, #07c160, #06ad56); color: #fff; text-align: center; font-size: 17px; font-weight: bold; }

.intro { padding: 12px 15px; background: #f9f9f9; }
.intro p { font-size: 13px; color: #666; margin-bottom: 8px; }
.tips { display: flex; flex-wrap: wrap; gap: 6px; }
.tips span { padding: 5px 10px; background: #e8f5e9; color: #07c160; border-radius: 15px; font-size: 12px; cursor: pointer; }

.chat-list { flex: 1; overflow-y: auto; padding: 12px; }
.msg { margin-bottom: 12px; display: flex; }
.msg.user { justify-content: flex-end; }
.msg.user .bubble { background: #07c160; color: #fff; }
.msg.ai .bubble { background: #f0f0f0; color: #333; }
.bubble { max-width: 80%; padding: 10px 14px; border-radius: 16px; font-size: 14px; line-height: 1.6; word-break: break-word; }
.typing { color: #999; font-style: italic; }

.input-bar { display: flex; padding: 10px 12px; gap: 8px; border-top: 1px solid #eee; background: #fff; }
.input-bar input { flex: 1; padding: 10px 14px; border: 1px solid #ddd; border-radius: 20px; font-size: 14px; outline: none; }
.input-bar button { padding: 10px 18px; background: #07c160; color: #fff; border: none; border-radius: 20px; font-size: 14px; cursor: pointer; white-space: nowrap; }
.input-bar button:disabled { background: #ccc; cursor: not-allowed; }
</style>
