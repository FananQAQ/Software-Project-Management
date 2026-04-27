<template>
  <div class="airport-panel">
    <!-- 头部 -->
    <div class="panel-header">
      <div class="header-left">
        <span class="airport-icon">🏢</span>
        <div>
          <div class="airport-name">{{ store.selectedAirport }}</div>
          <div class="airport-sub">国内民用机场</div>
        </div>
      </div>
      <button class="close-btn" @click="store.clearAirport()">✕</button>
    </div>

    <!-- 统计栏 -->
    <div class="stats-bar">
      <div class="stat-item">
        <span class="stat-num depart">{{ departing.length }}</span>
        <span class="stat-label">出发航班</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-num arrive">{{ arriving.length }}</span>
        <span class="stat-label">到达航班</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-num total">{{ departing.length + arriving.length }}</span>
        <span class="stat-label">共计</span>
      </div>
    </div>

    <!-- 标签切换 -->
    <div class="tab-bar">
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'depart' }"
        @click="activeTab = 'depart'"
      >出发 ({{ departing.length }})</button>
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'arrive' }"
        @click="activeTab = 'arrive'"
      >到达 ({{ arriving.length }})</button>
    </div>

    <!-- 航班列表 -->
    <div class="flight-list">
      <div v-if="currentList.length === 0" class="empty-tip">
        暂无{{ activeTab === 'depart' ? '出发' : '到达' }}航班
      </div>
      <div
        v-for="f in currentList"
        :key="f.flightId"
        class="flight-row"
        :class="{ selected: store.selectedFlight?.flightId === f.flightId }"
        @click="store.focusFlight(f.flightId)"
      >
        <div class="row-left">
          <span class="row-no">{{ f.flightNo }}</span>
          <span class="row-route">
            <template v-if="activeTab === 'depart'">→ {{ f.destination }}</template>
            <template v-else>← {{ f.origin }}</template>
          </span>
        </div>
        <div class="row-right">
          <span class="row-alt">{{ f.altitude }}m</span>
          <span class="row-speed">{{ f.speed }}km/h</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { flightStore as store } from '../../store/flightStore'

const activeTab = ref('depart')

const departing = computed(() => store.getAirportFlights(store.selectedAirport).departing)
const arriving  = computed(() => store.getAirportFlights(store.selectedAirport).arriving)
const currentList = computed(() => activeTab.value === 'depart' ? departing.value : arriving.value)
</script>

<style scoped>
.airport-panel {
  position: absolute;
  top: 16px;
  left: 16px;
  width: 280px;
  max-height: calc(100% - 32px);
  background: rgba(10, 14, 26, 0.93);
  border: 1px solid rgba(0, 229, 255, 0.25);
  border-radius: 10px;
  color: #e0e0e0;
  font-size: 13px;
  z-index: 1100;
  box-shadow: 0 6px 28px rgba(0, 0, 0, 0.45);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ── 头部 ── */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px 10px;
  border-bottom: 1px solid rgba(255,255,255,0.07);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.airport-icon {
  font-size: 22px;
  line-height: 1;
}

.airport-name {
  font-size: 15px;
  font-weight: 700;
  color: #00e5ff;
  letter-spacing: 0.5px;
}

.airport-sub {
  font-size: 11px;
  color: #555;
  margin-top: 1px;
}

.close-btn {
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
  line-height: 1;
  transition: color 0.2s;
}
.close-btn:hover { color: #fff; }

/* ── 统计栏 ── */
.stats-bar {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-num {
  font-size: 20px;
  font-weight: 700;
}
.stat-num.depart  { color: #76ff03; }
.stat-num.arrive  { color: #ff9500; }
.stat-num.total   { color: #00e5ff; }

.stat-label {
  font-size: 11px;
  color: #555;
}

.stat-divider {
  width: 1px;
  height: 28px;
  background: rgba(255,255,255,0.08);
  flex-shrink: 0;
}

/* ── 标签栏 ── */
.tab-bar {
  display: flex;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}

.tab-btn {
  flex: 1;
  padding: 8px 0;
  background: transparent;
  border: none;
  color: #666;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
  border-bottom: 2px solid transparent;
}

.tab-btn:hover { color: #ccc; }

.tab-btn.active {
  color: #00e5ff;
  border-bottom-color: #00e5ff;
}

/* ── 航班列表 ── */
.flight-list {
  overflow-y: auto;
  flex: 1;
  padding: 4px 0;
}

.flight-list::-webkit-scrollbar { width: 4px; }
.flight-list::-webkit-scrollbar-track { background: transparent; }
.flight-list::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.12); border-radius: 2px; }

.empty-tip {
  text-align: center;
  color: #444;
  font-size: 12px;
  padding: 20px 0;
}

.flight-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 7px 14px;
  cursor: pointer;
  transition: background 0.15s;
  border-left: 2px solid transparent;
}

.flight-row:hover {
  background: rgba(0, 229, 255, 0.07);
}

.flight-row.selected {
  background: rgba(230, 57, 70, 0.12);
  border-left-color: #e63946;
}

.row-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.row-no {
  font-size: 13px;
  font-weight: 600;
  color: #eee;
}

.row-route {
  font-size: 11px;
  color: #666;
}

.row-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.row-alt {
  font-size: 11px;
  color: #888;
}

.row-speed {
  font-size: 11px;
  color: #555;
}
</style>
