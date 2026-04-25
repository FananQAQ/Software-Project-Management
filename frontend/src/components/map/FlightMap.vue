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
let tracksLayer = null

// flightId -> L.Marker，保留引用以便直接 setLatLng 更新
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

// 俯视角飞机 SVG
function createPlaneIcon(heading, selected = false) {
  const color = selected ? '#00e5ff' : '#76ff03'
  const glow = selected ? 'drop-shadow(0 0 5px #00e5ff)' : 'drop-shadow(0 0 3px #76ff03)'
  const size = selected ? 34 : 26
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="${size}" height="${size}">
    <g transform="rotate(${heading},16,16)" style="filter:${glow}">
      <ellipse cx="16" cy="16" rx="2.5" ry="11" fill="${color}"/>
      <path d="M16 15 L3 23 L3 25 L16 20 L29 25 L29 23 Z" fill="${color}"/>
      <path d="M16 24 L11 30 L12 31 L16 27.5 L20 31 L21 30 Z" fill="${color}"/>
      <ellipse cx="16" cy="7" rx="1.5" ry="2" fill="white" opacity="0.6"/>
    </g>
  </svg>`
  return L.divIcon({ html: svg, className: '', iconSize: [size, size], iconAnchor: [size / 2, size / 2] })
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
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    subdomains: 'abcd', maxZoom: 18,
  }).addTo(map)
  L.control.zoom({ position: 'topright' }).addTo(map)
  tracksLayer = L.layerGroup().addTo(map)
}

// 全量初始化所有 marker（首次加载）
function initMarkers() {
  markerMap.forEach(m => m.remove())
  markerMap.clear()
  store.flights.forEach(f => {
    const selected = store.selectedFlight?.flightId === f.flightId
    const marker = L.marker([f.latitude, f.longitude], { icon: createPlaneIcon(f.heading, selected) })
    marker.on('click', () => store.selectFlight(f.flightId))
    marker.bindTooltip(f.flightNo, { permanent: false, direction: 'top', className: 'flight-tooltip', offset: [0, -12] })
    marker.addTo(map)
    markerMap.set(f.flightId, marker)
  })
}

// 每帧只更新 marker 位置（高性能）
function updateMarkerPositions() {
  store.flights.forEach(f => {
    const marker = markerMap.get(f.flightId)
    if (!marker) return
    marker.setLatLng([f.latitude, f.longitude])
    const selected = store.selectedFlight?.flightId === f.flightId
    marker.setIcon(createPlaneIcon(f.heading, selected))
  })
}

// 重绘所有轨迹线
function renderTracks() {
  tracksLayer.clearLayers()
  store.flights.forEach(f => {
    if (!f.track || f.track.length < 2) return
    const latLngs = f.track.map(p => [p.lat, p.lng])
    const selected = store.selectedFlight?.flightId === f.flightId
    if (selected) {
      for (let i = 1; i < latLngs.length; i++) {
        const opacity = 0.2 + (i / latLngs.length) * 0.8
        L.polyline([latLngs[i - 1], latLngs[i]], { color: '#00e5ff', weight: 2.5, opacity }).addTo(tracksLayer)
      }
    } else {
      L.polyline(latLngs, { color: '#76ff03', weight: 1, opacity: 0.22, dashArray: '4 6' }).addTo(tracksLayer)
    }
  })
}

async function refresh() {
  store.stopSimulation()
  await store.loadFlights()
  initMarkers()
  renderTracks()
  store.startSimulation()
}

function resetView() {
  map.setView([35, 105], 4)
}

onMounted(async () => {
  initMap()
  await store.loadFlights()
  initMarkers()
  renderTracks()
  store.startSimulation()
})

onUnmounted(() => {
  store.stopSimulation()
  map?.remove()
})

// 每次仿真 tick：只更新 marker 位置
watch(() => store.simTick, () => {
  updateMarkerPositions()
  // 每 5 tick 重绘轨迹，避免每帧都重绘
  if (store.simTick % 5 === 0) renderTracks()
})

// 选中变化时刷新图标颜色和轨迹高亮
watch(() => store.selectedFlight, () => {
  updateMarkerPositions()
  renderTracks()
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
  background: rgba(10,14,26,0.88);
  color: #aaa;
  border: 1px solid rgba(0,229,255,0.2);
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.ctrl-btn:hover {
  background: rgba(0,229,255,0.1);
  color: #00e5ff;
  border-color: rgba(0,229,255,0.5);
}

/* 流速控制栏 */
.speed-bar {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(8,12,24,0.88);
  border: 1px solid rgba(0,229,255,0.18);
  border-radius: 24px;
  padding: 6px 14px;
  z-index: 1000;
}

.speed-label {
  font-size: 11px;
  color: #555;
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
  color: #ccc;
  border-color: rgba(0,229,255,0.25);
}

.speed-btn.active {
  background: rgba(0,229,255,0.15);
  color: #00e5ff;
  border-color: rgba(0,229,255,0.5);
}

.map-legend {
  position: absolute;
  bottom: 24px;
  left: 14px;
  background: rgba(10,14,26,0.85);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 6px;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  z-index: 1000;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: #888;
}

.dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

.dot-fly { background: #76ff03; }
.dot-sel { background: #00e5ff; }
</style>
