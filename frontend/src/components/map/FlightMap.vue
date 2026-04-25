<template>
  <div class="map-wrapper">
    <div id="flight-map" ref="mapEl"></div>

    <!-- 加载提示 -->
    <div v-if="store.loading" class="map-loading">加载航班数据...</div>

    <!-- 左上：操作按钮 -->
    <div class="map-controls">
      <button class="ctrl-btn" @click="refresh">↻ 刷新</button>
      <button class="ctrl-btn" @click="resetView">⊙ 复位</button>
    </div>

    <!-- 底部中：时间流速控制栏 -->
    <div class="speed-bar">
      <span class="speed-label">仿真速率</span>
      <button
        v-for="s in speedOptions"
        :key="s.value"
        class="speed-btn"
        :class="{ active: store.simSpeed === s.value && !store.simPaused || (s.value === 0 && store.simPaused) }"
        @click="setSpeed(s.value)"
      >{{ s.label }}</button>
    </div>

    <!-- 左下：图例 -->
    <div class="map-legend">
      <span class="legend-item"><i class="dot dot-fly"></i>飞行中</span>
      <span class="legend-item"><i class="dot dot-sel"></i>已选中</span>
    </div>

    <!-- 航班详情弹窗 -->
    <FlightPopup
      v-if="store.selectedFlight"
      :flight="store.selectedFlight"
      @close="store.clearSelection()"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { flightStore as store } from '../../store/flightStore'
import FlightPopup from './FlightPopup.vue'

const mapEl = ref(null)
let map = null
let routesLayer  = null
let markersLayer = null

const markerMap = new Map()

const speedOptions = [
  { label: '⏸', value: 0 },
  { label: '1×', value: 1 },
  { label: '2×', value: 2 },
  { label: '5×', value: 5 },
  { label: '10×', value: 10 },
]

function setSpeed(v) {
  store.setSimSpeed(v)
}

// 俯视角飞机 SVG — pointer-events:none 保证点击穿透到 Leaflet marker
function createPlaneIcon(heading, selected = false) {
  const fill   = selected ? '#e63946' : '#1a1a2e'
  const stroke = '#ffffff'
  const glow   = selected
    ? 'drop-shadow(0 0 4px #e63946) drop-shadow(0 0 8px rgba(230,57,70,0.5))'
    : 'drop-shadow(1px 1px 2px rgba(0,0,0,0.55))'
  const size = selected ? 34 : 26
  const sw   = selected ? 1.2 : 0.9
  // pointer-events:none → 让点击穿透 SVG，由外层 div (Leaflet marker) 响应
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"
    width="${size}" height="${size}" style="pointer-events:none;display:block">
    <g transform="rotate(${heading},16,16)" style="filter:${glow}">
      <ellipse cx="16" cy="16" rx="2.5" ry="11" fill="${fill}" stroke="${stroke}" stroke-width="${sw}"/>
      <path d="M16 15 L3 23 L3 25 L16 20 L29 25 L29 23 Z" fill="${fill}" stroke="${stroke}" stroke-width="${sw}" stroke-linejoin="round"/>
      <path d="M16 24 L11 30 L12 31 L16 27.5 L20 31 L21 30 Z" fill="${fill}" stroke="${stroke}" stroke-width="${sw}" stroke-linejoin="round"/>
      <ellipse cx="16" cy="7" rx="1.2" ry="1.8" fill="white" opacity="0.7"/>
    </g>
  </svg>`
  return L.divIcon({
    html: `<div style="cursor:pointer;width:${size}px;height:${size}px">${svg}</div>`,
    className: '',
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
  })
}

function initMap() {
  map = L.map(mapEl.value, {
    center: [35, 105],
    zoom: 4,
    zoomControl: false,
    attributionControl: false,
    maxBounds: [[15, 68], [56, 140]],
    maxBoundsViscosity: 0.85,
  })
  L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
    subdomains: 'abcd', maxZoom: 18,
  }).addTo(map)
  L.control.zoom({ position: 'topright' }).addTo(map)
  routesLayer  = L.layerGroup().addTo(map)   // 航线虚线（起点→终点）：在下层
  markersLayer = L.layerGroup().addTo(map)   // 飞机图标：在上层
}

// ── 航线虚线（起点→终点）─────────────────────────────────────────
// flightId -> L.Polyline
const routeLineMap = new Map()

function syncRouteLines() {
  const activeIds = new Set()

  store.flights.forEach(f => {
    if (f.status === 'LANDING') {
      // 落地后移除航线
      const old = routeLineMap.get(f.flightId)
      if (old) { old.remove(); routeLineMap.delete(f.flightId) }
      return
    }
    activeIds.add(f.flightId)
    const selected = store.selectedFlight?.flightId === f.flightId
    const color    = selected ? '#e63946' : '#888888'
    const weight   = selected ? 2 : 1.2
    const opacity  = selected ? 0.75 : 0.4

    if (routeLineMap.has(f.flightId)) {
      // 只更新样式（起终点不变，不重建）
      const line = routeLineMap.get(f.flightId)
      line.setStyle({ color, weight, opacity })
    } else {
      // 新建航线
      const line = L.polyline(
        [[f.origLat, f.origLng], [f.destLat, f.destLng]],
        { color, weight, opacity, dashArray: '6 8', interactive: false }
      ).addTo(routesLayer)
      routeLineMap.set(f.flightId, line)
    }
  })

  // 清理已消失的（重生后 ID 复用，不会有残留）
}

// ── 飞机 Marker ────────────────────────────────────────────────────
function initMarkers() {
  markerMap.forEach(m => m.remove())
  markerMap.clear()
  store.flights.forEach(f => {
    const selected = store.selectedFlight?.flightId === f.flightId
    const marker = L.marker([f.latitude, f.longitude], {
      icon: createPlaneIcon(f.heading, selected),
      interactive: true,
    })
    marker.on('click', () => store.selectFlight(f.flightId))
    marker.bindTooltip(f.flightNo, {
      permanent: false, direction: 'top',
      className: 'flight-tooltip', offset: [0, -14],
    })
    marker.addTo(map)
    markerMap.set(f.flightId, marker)
  })
}

function updateMarkerPositions() {
  store.flights.forEach(f => {
    const marker = markerMap.get(f.flightId)
    if (!marker) return
    if (f.status === 'LANDING') {
      marker.setOpacity(0)
      return
    }
    marker.setOpacity(1)
    marker.setLatLng([f.latitude, f.longitude])
    const selected = store.selectedFlight?.flightId === f.flightId
    marker.setIcon(createPlaneIcon(f.heading, selected))
  })
}

async function refresh() {
  store.stopSimulation()
  routeLineMap.forEach(l => l.remove())
  routeLineMap.clear()
  await store.loadFlights()
  initMarkers()
  syncRouteLines()
  store.startSimulation()
}

function resetView() {
  map.setView([35, 105], 4)
}

onMounted(async () => {
  initMap()
  await store.loadFlights()
  initMarkers()
  syncRouteLines()
  store.startSimulation()
})

onUnmounted(() => {
  store.stopSimulation()
  map?.remove()
})

// 每 tick：更新位置；每 6 tick 同步一次航线（处理落地/重生）
watch(() => store.simTick, () => {
  updateMarkerPositions()
  if (store.simTick % 6 === 0) syncRouteLines()
})

// 选中变化：刷新图标颜色 + 航线高亮
watch(() => store.selectedFlight, () => {
  updateMarkerPositions()
  syncRouteLines()
})
</script>

<style scoped>
.map-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

#flight-map {
  width: 100%;
  height: 100%;
}

.map-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0,0,0,0.75);
  color: #76ff03;
  padding: 8px 18px;
  border-radius: 6px;
  font-size: 13px;
  z-index: 1000;
  pointer-events: none;
}

.map-controls {
  position: absolute;
  top: 14px;
  left: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  z-index: 1000;
}

.ctrl-btn {
  padding: 6px 14px;
  background: rgba(255,255,255,0.88);
  color: #333;
  border: 1px solid rgba(0,0,0,0.15);
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  box-shadow: 0 1px 4px rgba(0,0,0,0.15);
}

.ctrl-btn:hover {
  background: #fff;
  color: #e63946;
  border-color: #e63946;
}

/* 流速控制栏 */
.speed-bar {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255,255,255,0.92);
  border: 1px solid rgba(0,0,0,0.12);
  border-radius: 24px;
  padding: 5px 12px;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

.speed-label {
  font-size: 11px;
  color: #888;
  margin-right: 4px;
  white-space: nowrap;
}

.speed-btn {
  min-width: 36px;
  height: 28px;
  padding: 0 10px;
  background: transparent;
  color: #666;
  border: 1px solid transparent;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s;
}

.speed-btn:hover {
  color: #333;
  background: rgba(0,0,0,0.06);
}

.speed-btn.active {
  background: #e63946;
  color: #fff;
  border-color: #e63946;
}

.map-legend {
  position: absolute;
  bottom: 24px;
  left: 14px;
  background: rgba(255,255,255,0.92);
  border: 1px solid rgba(0,0,0,0.1);
  border-radius: 6px;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0,0,0,0.12);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: #555;
}

.dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

.dot-fly { background: #1a1a2e; border: 1.5px solid #fff; }
.dot-sel { background: #e63946; }
</style>
