<template>
  <div class="map-view">
    <!-- 顶部导航栏 -->
    <header class="topbar">
      <div class="topbar-left">
        <span class="logo">✈ 航班信息跟踪平台</span>
      </div>
      <div class="topbar-center">
        <span class="stat">在线航班&nbsp;<strong>{{ store.flights.length }}</strong></span>
        <span class="divider">|</span>
        <span class="stat">飞行中&nbsp;<strong>{{ inFlightCount }}</strong></span>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import FlightMap from '../components/map/FlightMap.vue'
import { flightStore as store } from '../store/flightStore'

const currentTime = ref('')
const inFlightCount = computed(() =>
  store.flights.filter(f => f.status === 'IN_FLIGHT').length
)

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
}

let timer = null
onMounted(() => { updateTime(); timer = setInterval(updateTime, 1000) })
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.map-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #050810;
  overflow: hidden;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 24px;
  background: rgba(3, 5, 15, 0.96);
  border-bottom: 1px solid rgba(0, 229, 255, 0.15);
  flex-shrink: 0;
  z-index: 2000;
}

.logo {
  font-size: 15px;
  font-weight: 700;
  color: #00e5ff;
  letter-spacing: 2px;
}

.topbar-center {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat {
  font-size: 12px;
  color: #555;
}

.stat strong {
  color: #76ff03;
  font-size: 14px;
}

.divider {
  color: #2a2a3a;
  font-size: 12px;
}

.time {
  font-size: 12px;
  color: #444;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
}

.map-body {
  flex: 1;
  position: relative;
  overflow: hidden;
}
</style>
