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
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; color: #333; }
a { text-decoration: none; color: inherit; }
#app-container { max-width: 480px; margin: 0 auto; min-height: 100vh; background: #fff; position: relative; padding-bottom: 60px; }

.tab-bar {
  position: fixed; bottom: 0; left: 50%; transform: translateX(-50%);
  width: 100%; max-width: 480px; height: 55px;
  background: #fff; border-top: 1px solid #eee;
  display: flex; justify-content: space-around; align-items: center;
  z-index: 100;
}
.tab-item {
  display: flex; flex-direction: column; align-items: center; font-size: 11px; color: #999;
}
.tab-item.active { color: #07c160; }
.tab-icon { font-size: 22px; margin-bottom: 2px; }
.tab-text { font-size: 10px; }
</style>
