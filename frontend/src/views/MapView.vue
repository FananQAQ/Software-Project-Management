<template>
  <div class="map-view">
    <!-- 顶部导航栏 -->
    <header class="topbar">
      <div class="topbar-left">
        <span class="logo">✈ 航班信息跟踪平台</span>
      </div>

      <!-- 搜索框 -->
      <div class="search-wrap" ref="searchWrapEl">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input
            class="search-input"
            v-model="store.searchQuery"
            placeholder="搜索航班号 / 出发地 / 目的地"
            @keydown.escape="store.searchQuery = ''"
            @keydown.enter="onEnter"
            autocomplete="off"
            spellcheck="false"
          />
          <button
            v-if="store.searchQuery"
            class="search-clear"
            @click="store.searchQuery = ''"
          >✕</button>
        </div>

        <!-- 下拉结果 -->
        <ul v-if="searchResults.length" class="search-dropdown">
          <li
            v-for="f in searchResults"
            :key="f.flightId"
            class="search-item"
            @click="onSelect(f)"
          >
            <span class="item-no">{{ f.flightNo }}</span>
            <span class="item-route">{{ f.origin }} → {{ f.destination }}</span>
            <span class="item-alt">{{ f.altitude }} m</span>
          </li>
        </ul>

        <!-- 无结果提示 -->
        <div v-else-if="store.searchQuery.trim()" class="search-empty">
          未找到匹配航班
        </div>
      </div>

      <!-- 起飞地筛选（OpenSky 模式：起飞地=最近机场近似） -->
      <div v-if="store.useOpenSky" class="origin-wrap" ref="originWrapEl">
        <button class="origin-btn" @click="originOpen = !originOpen">
          起飞地
          <span class="origin-count">{{ store.selectedOrigins.length }}</span>
          <span class="origin-caret">▾</span>
        </button>

        <div v-if="originOpen" class="origin-panel">
          <div class="origin-actions">
            <button class="mini-btn" @click="selectAllOrigins">全选</button>
            <button class="mini-btn danger" @click="clearOrigins">清空</button>
          </div>
          <div class="origin-list">
            <label v-for="a in airports" :key="a.name" class="origin-item">
              <input
                type="checkbox"
                :value="a.name"
                v-model="selectedOriginsLocal"
                @change="applyOrigins"
              />
              <span class="origin-name">{{ a.name }}</span>
            </label>
          </div>
        </div>
      </div>

      <div class="topbar-right">
        <span class="stat">在线&nbsp;<strong>{{ store.flights.length }}</strong></span>
        <span class="divider">|</span>
        <span class="stat">飞行中&nbsp;<strong>{{ inFlightCount }}</strong></span>
        <span class="divider">|</span>
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
import { flightStore as store, AIRPORTS } from '../store/flightStore'

/* ── 时钟 ─────────────────────────────────────────────────────── */
const currentTime = ref('')
const inFlightCount = computed(() =>
  store.flights.filter(f => f.status === 'IN_FLIGHT').length
)
function updateTime() {
  currentTime.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
}
let timer = null
onMounted(() => { updateTime(); timer = setInterval(updateTime, 1000) })
onUnmounted(() => clearInterval(timer))

/* ── 搜索 ─────────────────────────────────────────────────────── */
const searchWrapEl = ref(null)
const originWrapEl = ref(null)
const originOpen = ref(false)
const airports = AIRPORTS
const selectedOriginsLocal = ref([...store.selectedOrigins])

function applyOrigins() {
  store.setSelectedOrigins([...selectedOriginsLocal.value])
}

function clearOrigins() {
  selectedOriginsLocal.value = []
  applyOrigins()
}

function selectAllOrigins() {
  selectedOriginsLocal.value = airports.map(a => a.name)
  applyOrigins()
}

const searchResults = computed(() => {
  const q = store.searchQuery.trim().toLowerCase()
  if (!q) return []
  return store.flights
    .filter(f =>
      f.status !== 'LANDING' && (
        f.flightNo.toLowerCase().includes(q) ||
        f.origin.toLowerCase().includes(q) ||
        f.destination.toLowerCase().includes(q)
      )
    )
    .slice(0, 8)
})

function onSelect(f) {
  store.focusFlight(f.flightId)
}

function onEnter() {
  if (searchResults.value.length > 0) {
    onSelect(searchResults.value[0])
  }
}

// 点击搜索框外部时清空搜索词（关闭下拉）
function onClickOutside(e) {
  if (searchWrapEl.value && !searchWrapEl.value.contains(e.target)) {
    store.searchQuery = ''
  }
  if (originWrapEl.value && !originWrapEl.value.contains(e.target)) {
    originOpen.value = false
  }
}
onMounted(() => document.addEventListener('mousedown', onClickOutside))
onUnmounted(() => document.removeEventListener('mousedown', onClickOutside))
</script>

<style scoped>
.map-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
  overflow: hidden;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 20px;
  background: #1a1a2e;
  border-bottom: 2px solid #e63946;
  flex-shrink: 0;
  z-index: 2000;
  gap: 16px;
}

.logo {
  font-size: 14px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 2px;
  white-space: nowrap;
}

/* ── 搜索区域 ────────────────────────────────────────────────── */
.search-wrap {
  position: relative;
  flex: 1;
  max-width: 360px;
}

.search-box {
  display: flex;
  align-items: center;
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 20px;
  padding: 0 12px;
  height: 32px;
  gap: 6px;
  transition: border-color 0.2s, background 0.2s;
}

.search-box:focus-within {
  border-color: rgba(230,57,70,0.6);
  background: rgba(255,255,255,0.12);
}

.search-icon {
  font-size: 13px;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #eee;
  font-size: 12px;
  min-width: 0;
}

.search-input::placeholder {
  color: #666;
}

.search-clear {
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  font-size: 11px;
  padding: 0;
  line-height: 1;
  flex-shrink: 0;
}

.search-clear:hover { color: #ccc; }

/* ── 下拉列表 ────────────────────────────────────────────────── */
.search-dropdown,
.search-empty {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  background: #1e2235;
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.4);
  z-index: 3000;
  overflow: hidden;
  list-style: none;
  margin: 0;
  padding: 4px 0;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  cursor: pointer;
  transition: background 0.15s;
}

.search-item:hover {
  background: rgba(230,57,70,0.15);
}

.item-no {
  font-size: 13px;
  font-weight: 600;
  color: #00e5ff;
  min-width: 64px;
}

.item-route {
  flex: 1;
  font-size: 12px;
  color: #bbb;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-alt {
  font-size: 11px;
  color: #666;
  white-space: nowrap;
}

.search-empty {
  padding: 10px 14px;
  font-size: 12px;
  color: #666;
  text-align: center;
}

/* ── 右侧统计 ────────────────────────────────────────────────── */
.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.stat {
  font-size: 12px;
  color: #aaa;
}

.stat strong {
  color: #ff9500;
  font-size: 13px;
}

.divider {
  color: #444;
  font-size: 12px;
}

.time {
  font-size: 12px;
  color: #888;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
}

.map-body {
  flex: 1;
  position: relative;
  overflow: hidden;
}

/* ── 起飞地筛选（OpenSky） ───────────────────────────────────── */
.origin-wrap {
  position: relative;
  flex-shrink: 0;
}

.origin-btn {
  height: 32px;
  padding: 0 12px;
  border-radius: 20px;
  border: 1px solid rgba(255,255,255,0.15);
  background: rgba(255,255,255,0.08);
  color: #eee;
  font-size: 12px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}
.origin-btn:hover {
  background: rgba(255,255,255,0.12);
}

.origin-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(0,229,255,0.15);
  border: 1px solid rgba(0,229,255,0.25);
  color: #00e5ff;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

.origin-caret { color: #666; font-size: 11px; }

.origin-panel {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  width: 260px;
  background: #1e2235;
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.4);
  z-index: 3100;
  overflow: hidden;
}

.origin-actions {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}

.mini-btn {
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid rgba(255,255,255,0.12);
  background: rgba(255,255,255,0.06);
  color: #ccc;
  font-size: 12px;
  cursor: pointer;
}
.mini-btn:hover { background: rgba(255,255,255,0.10); color: #fff; }
.mini-btn.danger:hover { border-color: rgba(230,57,70,0.5); color: #e63946; }

.origin-list {
  max-height: 280px;
  overflow: auto;
  padding: 8px 10px 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.origin-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 8px;
  cursor: pointer;
  user-select: none;
}
.origin-item:hover { background: rgba(230,57,70,0.12); }

.origin-item input {
  accent-color: #e63946;
}

.origin-name {
  font-size: 12px;
  color: #ddd;
}
</style>
