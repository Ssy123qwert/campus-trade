<template>
  <div class="stats-page">
    <header class="header">
      <span class="back" @click="$router.back()">← 返回</span>
      <span>数据统计看板</span>
      <span style="width:40px"></span>
    </header>

    <!-- 概览卡片 -->
    <div class="overview-row">
      <div v-for="card in overviewCards" :key="card.label" class="overview-card" :style="{ background: card.bg }">
        <span class="ov-icon">{{ card.icon }}</span>
        <div class="ov-info">
          <span class="ov-num">{{ card.value }}</span>
          <span class="ov-label">{{ card.label }}</span>
        </div>
      </div>
    </div>

    <!-- 用户/订单趋势 -->
    <div class="chart-card">
      <div class="chart-header">
        <span class="chart-title">增长趋势</span>
        <select v-model="trendDays" @change="loadTrend" class="chart-select">
          <option :value="7">近7天</option>
          <option :value="30">近30天</option>
        </select>
      </div>
      <div ref="trendChart" class="chart-box"></div>
    </div>

    <!-- 分类占比 + 热门商品 -->
    <div class="chart-row">
      <div class="chart-card half">
        <div class="chart-header">
          <span class="chart-title">分类占比</span>
        </div>
        <div ref="categoryChart" class="chart-box-sm"></div>
      </div>
      <div class="chart-card half">
        <div class="chart-header">
          <span class="chart-title">热门商品 TOP10</span>
        </div>
        <div ref="hotChart" class="chart-box-sm"></div>
      </div>
    </div>

    <!-- 操作日志 -->
    <div class="chart-card">
      <div class="chart-header">
        <span class="chart-title">最近操作日志</span>
      </div>
      <div class="log-list" v-if="logs.length > 0">
        <div v-for="log in logs" :key="log.id" class="log-item">
          <span class="log-user">{{ log.username }}</span>
          <span class="log-action">{{ log.operation }}</span>
          <span :class="['log-result', log.result === 'SUCCESS' ? 'ok' : 'fail']">{{ log.result }}</span>
          <span class="log-time">{{ log.createTime }}</span>
        </div>
      </div>
      <div v-else class="empty-chart">暂无操作日志</div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { api } from '../api'
import * as echarts from 'echarts'

export default {
  name: 'Statistics',
  setup() {
    const overviewCards = ref([])
    const trendDays = ref(7)
    const logs = ref([])

    const trendChart = ref(null)
    const categoryChart = ref(null)
    const hotChart = ref(null)

    let trendInstance = null
    let categoryInstance = null
    let hotInstance = null

    // ===== 渐变色工具 =====
    const greenGradient = (opacity = 1) => ({
      type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
      colorStops: [
        { offset: 0, color: `rgba(7, 193, 96, ${opacity})` },
        { offset: 1, color: `rgba(7, 193, 96, 0.05)` }
      ]
    })

    const blueGradient = (opacity = 1) => ({
      type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
      colorStops: [
        { offset: 0, color: `rgba(64, 158, 255, ${opacity})` },
        { offset: 1, color: `rgba(64, 158, 255, 0.05)` }
      ]
    })

    const goldGradient = {
      type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
      colorStops: [
        { offset: 0, color: '#f59e0b' },
        { offset: 1, color: '#f97316' }
      ]
    }

    // ===== 加载概览 =====
    const loadOverview = async () => {
      const res = await api.getStatsOverview()
      if (res.code !== 200) return
      const d = res.data
      overviewCards.value = [
        { icon: '👥', label: '用户总数', value: d.userCount, bg: 'linear-gradient(135deg, #667eea, #764ba2)' },
        { icon: '📦', label: '商品总数', value: d.productCount, bg: 'linear-gradient(135deg, #f093fb, #f5576c)' },
        { icon: '📋', label: '订单总数', value: d.orderCount, bg: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
        { icon: '💰', label: '今日交易额', value: '¥' + (d.todayRevenue || 0), bg: 'linear-gradient(135deg, #43e97b, #38f9d7)' }
      ]
    }

    // ===== 加载趋势 =====
    const loadTrend = async () => {
      const [userRes, orderRes] = await Promise.all([
        api.getUserTrend(trendDays.value),
        api.getOrderTrend(trendDays.value)
      ])
      const userData = userRes
      const orderData = orderRes

      if (userData.code !== 200 || orderData.code !== 200) return

      const dates = userData.data.map(d => d.date?.slice(5) || '')
      const users = userData.data.map(d => d.count)
      const orders = orderData.data.map(d => d.count)
      const completed = orderData.data.map(d => d.completed)

      await nextTick()
      if (trendInstance) trendInstance.dispose()
      trendInstance = echarts.init(trendChart.value)

      trendInstance.setOption({
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255,255,255,0.95)',
          borderColor: '#07c160',
          borderWidth: 2,
          padding: [10, 14],
          formatter: (params) => {
            let html = `<div style="font-weight:bold;margin-bottom:6px;color:#333">${params[0].axisValue}</div>`
            params.forEach(p => {
              html += `<div style="display:flex;align-items:center;gap:6px;margin:3px 0">
                <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${p.color}"></span>
                <span style="color:#666">${p.seriesName}：</span>
                <span style="font-weight:bold;color:#333">${p.value}</span>
              </div>`
            })
            return html
          }
        },
        legend: {
          data: ['新增用户', '新增订单', '已完成'],
          bottom: 0,
          icon: 'circle',
          itemWidth: 8
        },
        grid: { left: 40, right: 16, top: 10, bottom: 40 },
        xAxis: {
          type: 'category',
          data: dates,
          axisLine: { lineStyle: { color: '#eee' } },
          axisLabel: { fontSize: 10, color: '#999' }
        },
        yAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: '#f5f5f5', type: 'dashed' } }
        },
        series: [
          {
            name: '新增用户',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: { width: 3, color: '#667eea' },
            areaStyle: { ...blueGradient(0.3) },
            itemStyle: { color: '#667eea' },
            data: users
          },
          {
            name: '新增订单',
            type: 'line',
            smooth: true,
            symbol: 'diamond',
            symbolSize: 6,
            lineStyle: { width: 3, color: '#07c160' },
            areaStyle: { ...greenGradient(0.3) },
            itemStyle: { color: '#07c160' },
            data: orders
          },
          {
            name: '已完成',
            type: 'bar',
            barWidth: 8,
            itemStyle: {
              color: goldGradient,
              borderRadius: [4, 4, 0, 0]
            },
            data: completed
          }
        ]
      })
    }

    // ===== 分类占比 =====
    const loadCategory = async () => {
      const res = await api.getCategoryDistribution()
      if (res.code !== 200) return

      const colors = ['#07c160', '#409eff', '#f59e0b', '#f56c6c', '#9b59b6', '#1abc9c', '#e74c3c', '#3498db']
      await nextTick()
      if (categoryInstance) categoryInstance.dispose()
      categoryInstance = echarts.init(categoryChart.value)

      categoryInstance.setOption({
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(255,255,255,0.95)',
          borderColor: '#07c160',
          borderWidth: 2,
          formatter: (p) => `
            <div style="font-weight:bold;color:#333">${p.name}</div>
            <div style="margin-top:4px">数量：<span style="font-weight:bold;color:#07c160">${p.value}</span> 件</div>
            <div>占比：<span style="font-weight:bold">${p.percent}%</span></div>
          `
        },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: true,
          padAngle: 2,
          itemStyle: {
            borderRadius: 6,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: true,
            formatter: '{b}\n{d}%',
            fontSize: 11,
            color: '#666'
          },
          emphasis: {
            label: { show: true, fontSize: 14, fontWeight: 'bold' },
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0,0,0,0.2)'
            }
          },
          data: res.data.map((d, i) => ({
            ...d,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                { offset: 0, color: colors[i % colors.length] },
                { offset: 1, color: colors[(i + 1) % colors.length] }
              ])
            }
          }))
        }]
      })
    }

    // ===== 热门商品 =====
    const loadHot = async () => {
      const res = await api.getHotProducts()
      if (res.code !== 200) return

      await nextTick()
      if (hotInstance) hotInstance.dispose()
      hotInstance = echarts.init(hotChart.value)

      const titles = res.data.map(d => d.title?.length > 8 ? d.title.slice(0, 8) + '..' : d.title || '未知')
      const views = res.data.map(d => d.viewCount)

      hotInstance.setOption({
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255,255,255,0.95)',
          borderColor: '#f59e0b',
          borderWidth: 2,
          formatter: (params) => {
            const d = res.data[params[0].dataIndex]
            return `
              <div style="font-weight:bold;color:#333;max-width:200px;overflow:hidden;text-overflow:ellipsis">${d.title}</div>
              <div style="margin-top:4px">浏览量：<span style="font-weight:bold;color:#f59e0b">${d.viewCount}</span></div>
            `
          }
        },
        grid: { left: 8, right: 40, top: 5, bottom: 8 },
        xAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: '#f5f5f5', type: 'dashed' } },
          axisLabel: { fontSize: 10, color: '#999' }
        },
        yAxis: {
          type: 'category',
          data: titles.reverse(),
          axisLine: { show: false },
          axisTick: { show: false },
          axisLabel: { fontSize: 10, color: '#333' }
        },
        series: [{
          type: 'bar',
          data: views.reverse().map((v, i) => ({
            value: v,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: `rgba(7, 193, 96, ${0.3 + i * 0.07})` },
                { offset: 1, color: `rgba(7, 193, 96, ${0.6 + i * 0.04})` }
              ]),
              borderRadius: [0, 6, 6, 0]
            }
          })),
          barWidth: 14,
          label: {
            show: true,
            position: 'right',
            fontSize: 10,
            color: '#999'
          }
        }]
      })
    }

    const loadLogs = async () => {
      const res = await api.getOperationLogs()
      if (res.code === 200) logs.value = res.data
    }

    onMounted(async () => {
      await loadOverview()
      await loadTrend()
      await loadCategory()
      await loadHot()
      await loadLogs()
    })

    onBeforeUnmount(() => {
      trendInstance?.dispose()
      categoryInstance?.dispose()
      hotInstance?.dispose()
    })

    return { overviewCards, trendDays, trendChart, categoryChart, hotChart, logs, loadTrend }
  }
}
</script>

<style scoped>
.stats-page { padding-bottom: 20px; }
.header { display: flex; align-items: center; justify-content: space-between; padding: 12px 15px; background: #fff; border-bottom: 1px solid #eee; font-size: 16px; position: sticky; top: 0; z-index: 10; }
.back { color: #07c160; cursor: pointer; }

.overview-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; padding: 12px; }
.overview-card { border-radius: 14px; padding: 16px; display: flex; align-items: center; gap: 14px; color: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.ov-icon { font-size: 28px; }
.ov-num { display: block; font-size: 22px; font-weight: 700; }
.ov-label { font-size: 11px; opacity: 0.85; }

.chart-card { background: #fff; border-radius: 14px; margin: 0 12px 12px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.chart-title { font-size: 15px; font-weight: 600; color: #333; }
.chart-select { padding: 4px 8px; border: 1px solid #ddd; border-radius: 6px; font-size: 12px; outline: none; }
.chart-box { height: 240px; }
.chart-box-sm { height: 220px; }
.chart-row { display: flex; gap: 0; }
.half { flex: 1; margin: 0 0 12px 12px; }
.half:last-child { margin-right: 12px; }
.empty-chart { text-align: center; padding: 40px; color: #999; font-size: 13px; }

.log-list { max-height: 300px; overflow-y: auto; }
.log-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid #f5f5f5; font-size: 12px; }
.log-user { color: #07c160; font-weight: 600; min-width: 50px; }
.log-action { flex: 1; color: #333; }
.log-result { padding: 1px 8px; border-radius: 4px; font-size: 11px; }
.log-result.ok { background: #e8f5e9; color: #07c160; }
.log-result.fail { background: #ffebee; color: #f44; }
.log-time { color: #bbb; font-size: 11px; min-width: 80px; text-align: right; }
</style>
