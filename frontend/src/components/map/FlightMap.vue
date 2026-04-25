<template>
  <div class="map-wrapper">
    <div id="flight-map" ref="mapEl"></div>

    <!-- 加载提示 -->
    <div v-if="store.loading" class="map-loading">
      <span>加载航班数据...</span>
    </div>

    <!-- 刷新按钮 -->
    <div class="map-controls">
      <button class="ctrl-btn" @click="refresh" title="刷新航班">↻</button>
      <button class="ctrl-btn" @click="resetView" title="复位视图">⊙</button>
    </div>

    <!-- 图例 -->
    <div class="map-legend">
      <span class="legend-item"><i class="dot dot-fly"></i> 飞行中</span>
      <span class="legend-item selected-item"><i class="dot dot-sel"></i> 已选中</span>
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
let markersLayer = null
let trackLayer = null
let refreshTimer = null

// SVG 飞机图标，根据航向旋转
function createPlaneIcon(heading, selected = false) {
  const color = selected ? '#00e5ff' : '#76ff03'
  const size = selected ? 32 : 24
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="${size}" height="${size}">
    <g transform="rotate(${heading}, 12, 12)">
      <path d="M12 2 L15 9 L22 10 L17 15 L18.5 22 L12 19 L5.5 22 L7 15 L2 10 L9 9 Z"
        fill="${color}" stroke="#fff" stroke-width="0.8" opacity="0.95"/>
    </g>
  </svg>`
  return L.divIcon({
    html: svg,
    className: '',
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
  })
}

function initMap() {
  map = L.map(mapEl.value, {
    center: [35, 110],
    zoom: 5,
    zoomControl: false,
  })

  // 深色底图（CartoDB Dark Matter）
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; <a href="https://carto.com/">CARTO</a>',
    subdomains: 'abcd',
    maxZoom: 18,
  }).addTo(map)

  L.control.zoom({ position: 'topright' }).addTo(map)

  markersLayer = L.layerGroup().addTo(map)
  trackLayer = L.layerGroup().addTo(map)
}

function renderFlights() {
  markersLayer.clearLayers()

  store.flights.forEach(flight => {
    const selected = store.selectedFlight?.flightId === flight.flightId
    const icon = createPlaneIcon(flight.heading || 0, selected)
    const marker = L.marker([flight.latitude, flight.longitude], { icon })

    marker.on('click', () => {
      store.selectFlight(flight.flightId)
    })

    marker.bindTooltip(`${flight.flightNo}`, {
      permanent: false,
      direction: 'top',
      className: 'flight-tooltip',
      offset: [0, -10],
    })

    markersLayer.addLayer(marker)
  })
}

function renderTrack() {
  trackLayer.clearLayers()
  if (!store.selectedTrack || store.selectedTrack.length < 2) return

  const latLngs = store.selectedTrack.map(p => [p.latitude, p.longitude])

  // 尾迹渐变效果：用多段折线叠加
  for (let i = 1; i < latLngs.length; i++) {
    const opacity = 0.2 + (i / latLngs.length) * 0.7
    L.polyline([latLngs[i - 1], latLngs[i]], {
      color: '#00e5ff',
      weight: 2,
      opacity,
    }).addTo(trackLayer)
  }
}

async function refresh() {
  await store.loadFlights()
  renderFlights()
}

function resetView() {
  map.setView([35, 110], 5)
}

onMounted(async () => {
  initMap()
  await store.loadFlights()
  renderFlights()
  // 每 30 秒自动刷新
  refreshTimer = setInterval(refresh, 30000)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
  map?.remove()
})

// 选中航班变化时重渲染标记和轨迹
watch(
  () => store.selectedFlight,
  () => {
    renderFlights()
    renderTrack()
  }
)
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
  background: rgba(0, 0, 0, 0.7);
  color: #76ff03;
  padding: 10px 20px;
  border-radius: 6px;
  font-size: 14px;
  z-index: 1000;
}

.map-controls {
  position: absolute;
  top: 16px;
  left: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  z-index: 1000;
}

.ctrl-btn {
  width: 36px;
  height: 36px;
  background: rgba(20, 20, 30, 0.85);
  color: #e0e0e0;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.ctrl-btn:hover {
  background: rgba(118, 255, 3, 0.2);
  color: #76ff03;
}

.map-legend {
  position: absolute;
  bottom: 28px;
  left: 16px;
  background: rgba(10, 10, 20, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
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
  gap: 8px;
  font-size: 12px;
  color: #b0b0b0;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.dot-fly { background: #76ff03; }
.dot-sel { background: #00e5ff; }
</style>
