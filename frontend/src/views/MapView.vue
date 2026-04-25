<template>
  <div class="map-view">
    <!-- 顶部导航栏 -->
    <header class="topbar">
      <div class="topbar-left">
        <span class="logo">✈ 航班信息跟踪平台</span>
      </div>
      <div class="topbar-center">
        <span class="stat">
          在线航班：<strong>{{ store.flights.length }}</strong>
        </span>
      </div>
      <div class="topbar-right">
        <span class="time">{{ currentTime }}</span>
      </div>
    </header>

    <!-- 地图主体 -->
    <main class="map-body">
      <FlightMap />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import FlightMap from '../components/map/FlightMap.vue'
import { flightStore as store } from '../store/flightStore'

const currentTime = ref('')

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
}

let timer = null
onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.map-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #0a0e1a;
  overflow: hidden;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 20px;
  background: rgba(5, 8, 18, 0.95);
  border-bottom: 1px solid rgba(0, 229, 255, 0.2);
  flex-shrink: 0;
  z-index: 2000;
}

.logo {
  font-size: 16px;
  font-weight: 700;
  color: #00e5ff;
  letter-spacing: 2px;
}

.stat {
  font-size: 13px;
  color: #888;
}

.stat strong {
  color: #76ff03;
  font-size: 15px;
}

.time {
  font-size: 13px;
  color: #666;
  font-variant-numeric: tabular-nums;
}

.map-body {
  flex: 1;
  position: relative;
  overflow: hidden;
}
</style>
