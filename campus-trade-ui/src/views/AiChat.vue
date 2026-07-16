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
import { ref, nextTick } from 'vue'

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
        // 优先用后端，后端不通则浏览器直连 DeepSeek
        let content = null
        try {
          const res = await fetch('/api/ai/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ question: text })
          })
          const data = await res.json()
          if (data.code === 200 || res.ok) {
            content = data.data || data.msg || null
          }
        } catch (_) { /* 后端不通，走直连 */ }

        if (!content) {
          const deepRes = await fetch('https://api.deepseek.com/v1/chat/completions', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': 'Bearer import.meta.env.VITE_DEEPSEEK_KEY'
            },
            body: JSON.stringify({
              model: 'deepseek-chat',
              messages: [
                { role: 'system', content: '你是校园二手交易平台的AI助手。你的职责是：1. 为用户提供二手商品选购建议 2. 帮助用户评估商品合理价格 3. 提供二手交易注意事项和安全建议 4. 回答关于校园二手交易的各种问题。请用友好、专业的语气回答，每次回答控制在200字以内。' },
                { role: 'user', content: text }
              ],
              max_tokens: 500,
              temperature: 0.7
            })
          })
          const deepData = await deepRes.json()
          content = deepData.choices?.[0]?.message?.content || 'AI已收到你的问题'
        }
        messages.value.push({ role: 'ai', content })
      } catch (e) {
        messages.value.push({ role: 'ai', content: 'AI服务连接失败，请检查网络后重试。' })
      }
      loading.value = false
      await scrollBottom()
    }

    const ask = (q) => { input.value = q; send() }
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

/* 平板/桌面: 聊天区居中 + 对话气泡加宽 */
@media (min-width: 768px) {
  .ai-page { max-width: 800px; margin: 0 auto; height: calc(100vh - 75px); }
  .header { font-size: 20px; padding: 18px; border-radius: 0 0 16px 16px; }
  .intro { padding: 16px 24px; }
  .intro p { font-size: 14px; }
  .tips span { font-size: 13px; padding: 8px 16px; }
  .chat-list { padding: 16px 24px; }
  .bubble { max-width: 65%; font-size: 15px; padding: 14px 20px; }
  .input-bar { padding: 14px 24px; }
  .input-bar input { padding: 14px 18px; font-size: 15px; }
  .input-bar button { padding: 14px 24px; font-size: 15px; }
}
</style>