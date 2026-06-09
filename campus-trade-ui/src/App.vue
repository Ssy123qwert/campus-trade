<template>
  <div id="app-container">
    <router-view />
    <nav class="tab-bar" v-if="showTabBar">
      <router-link to="/" class="tab-item" :class="{ active: $route.path === '/' }">
        <span class="tab-icon">🏠</span>
        <span class="tab-text">首页</span>
      </router-link>
      <router-link to="/publish" class="tab-item" :class="{ active: $route.path === '/publish' }">
        <span class="tab-icon">✏️</span>
        <span class="tab-text">发布</span>
      </router-link>
      <router-link to="/ai-chat" class="tab-item" :class="{ active: $route.path === '/ai-chat' }">
        <span class="tab-icon">🤖</span>
        <span class="tab-text">AI助手</span>
      </router-link>
      <router-link to="/orders" class="tab-item" :class="{ active: $route.path === '/orders' }">
        <span class="tab-icon">📋</span>
        <span class="tab-text">订单</span>
      </router-link>
      <router-link to="/profile" class="tab-item" :class="{ active: $route.path === '/profile' }">
        <span class="tab-icon">👤</span>
        <span class="tab-text">我的</span>
      </router-link>
    </nav>
  </div>
</template>

<script>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

export default {
  name: 'App',
  setup() {
    const route = useRoute()
    const showTabBar = computed(() => {
      return !['/login', '/register', '/detail', '/chat', '/messages', '/review', '/reviews'].some(p => route.path.startsWith(p))
    })
    return { showTabBar }
  }
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif; background: #f5f5f7; color: #333; -webkit-font-smoothing: antialiased; }
a { text-decoration: none; color: inherit; }
#app-container { max-width: 480px; margin: 0 auto; min-height: 100vh; background: #f5f5f7; position: relative; padding-bottom: 65px; }

.tab-bar {
  position: fixed; bottom: 0; left: 50%; transform: translateX(-50%);
  width: 100%; max-width: 480px; height: 60px;
  background: #fff; border-top: none;
  display: flex; justify-content: space-around; align-items: center;
  z-index: 100; box-shadow: 0 -2px 12px rgba(0,0,0,0.06);
  border-radius: 16px 16px 0 0;
}
.tab-item {
  display: flex; flex-direction: column; align-items: center; font-size: 11px; color: #999;
  padding: 6px 0; transition: all 0.2s; position: relative;
}
.tab-item.active { color: #07c160; }
.tab-item.active::after {
  content: ''; position: absolute; top: 0; left: 50%; transform: translateX(-50%);
  width: 20px; height: 3px; background: #07c160; border-radius: 0 0 3px 3px;
}
.tab-icon { font-size: 22px; margin-bottom: 2px; }
.tab-text { font-size: 10px; font-weight: 500; }

/* 全局通用样式 */
.page-header {
  padding: 15px 16px; background: linear-gradient(135deg, #07c160, #06ad56);
  color: #fff; font-size: 17px; font-weight: 600; text-align: center; position: sticky; top: 0; z-index: 50;
}
.page-header .back { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); background: none; border: none; color: #fff; font-size: 18px; cursor: pointer; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); overflow: hidden; }
.btn-primary { background: linear-gradient(135deg, #07c160, #06ad56); color: #fff; border: none; border-radius: 10px; padding: 14px; font-size: 16px; font-weight: 500; cursor: pointer; transition: all 0.2s; }
.btn-primary:active { transform: scale(0.98); opacity: 0.9; }
.input-field { padding: 14px 16px; border: 1.5px solid #eee; border-radius: 12px; font-size: 15px; outline: none; transition: border-color 0.2s; background: #fafafa; }
.input-field:focus { border-color: #07c160; background: #fff; }
</style>
