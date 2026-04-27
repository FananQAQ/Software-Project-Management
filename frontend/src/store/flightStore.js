import { reactive } from 'vue'
import { fetchFlights, fetchFlightDetail, fetchFlightTrack } from '../api/flightApi'

export const AIRPORTS = [
  { name: '北京首都',       lat: 40.08, lng: 116.58 },
  { name: '北京大兴',       lat: 39.51, lng: 116.41 },
  { name: '上海浦东',       lat: 31.14, lng: 121.80 },
  { name: '上海虹桥',       lat: 31.20, lng: 121.34 },
  { name: '广州白云',       lat: 23.39, lng: 113.30 },
  { name: '深圳宝安',       lat: 22.64, lng: 113.81 },
  { name: '成都天府',       lat: 30.32, lng: 104.44 },
  { name: '成都双流',       lat: 30.58, lng: 103.95 },
  { name: '重庆江北',       lat: 29.72, lng: 106.64 },
  { name: '昆明长水',       lat: 25.10, lng: 102.93 },
  { name: '西安咸阳',       lat: 34.44, lng: 108.75 },
  { name: '武汉天河',       lat: 30.78, lng: 114.21 },
  { name: '杭州萧山',       lat: 30.23, lng: 120.43 },
  { name: '南京禄口',       lat: 31.74, lng: 118.86 },
  { name: '郑州新郑',       lat: 34.52, lng: 113.84 },
  { name: '长沙黄花',       lat: 28.19, lng: 113.22 },
  { name: '青岛胶东',       lat: 36.36, lng: 120.08 },
  { name: '厦门高崎',       lat: 24.54, lng: 118.13 },
  { name: '哈尔滨太平',     lat: 45.62, lng: 126.25 },
  { name: '乌鲁木齐地窝堡', lat: 43.91, lng: 87.47  },
  { name: '拉萨贡嘎',       lat: 29.30, lng: 90.91  },
  { name: '海口美兰',       lat: 19.94, lng: 110.46 },
  { name: '三亚凤凰',       lat: 18.30, lng: 109.41 },
  { name: '沈阳桃仙',       lat: 41.64, lng: 123.49 },
  { name: '大连周水子',     lat: 38.97, lng: 121.54 },
  { name: '天津滨海',       lat: 39.12, lng: 117.35 },
  { name: '南宁吴圩',       lat: 22.61, lng: 108.17 },
  { name: '贵阳龙洞堡',     lat: 26.54, lng: 106.80 },
  { name: '福州长乐',       lat: 25.93, lng: 119.66 },
  { name: '兰州中川',       lat: 36.52, lng: 103.62 },
]
const AIRLINES = ['CA', 'MU', 'CZ', 'HU', 'FM', 'ZH', 'MF', 'SC', '3U', 'GS', 'KN', 'BK']

function haversineKm(lat1, lng1, lat2, lng2) {
  const R = 6371
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLng = (lng2 - lng1) * Math.PI / 180
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

function pickRoute() {
  let oi = Math.floor(Math.random() * AIRPORTS.length)
  let di = Math.floor(Math.random() * AIRPORTS.length)
  while (di === oi) di = Math.floor(Math.random() * AIRPORTS.length)
  return { orig: AIRPORTS[oi], dest: AIRPORTS[di] }
}

/** 根据起终点和当前进度 [0,1] 创建航班对象 */
function buildFlight(id, orig, dest, progress = 0) {
  const heading = ((Math.atan2(dest.lng - orig.lng, dest.lat - orig.lat) * 180) / Math.PI + 360) % 360
  const lat = orig.lat + (dest.lat - orig.lat) * progress
  const lng = orig.lng + (dest.lng - orig.lng) * progress
  return {
    flightId:    `MOCK${String(id).padStart(3, '0')}`,
    flightNo:    `${AIRLINES[id % AIRLINES.length]}${1000 + id}`,
    origLat:     orig.lat,
    origLng:     orig.lng,
    destLat:     dest.lat,
    destLng:     dest.lng,
    origin:      orig.name,
    destination: dest.name,
    progress,             // 0 = 出发地, 1 = 目的地
    latitude:    lat,
    longitude:   lng,
    altitude:    Math.floor(6000 + Math.random() * 5000),
    speed:       Math.floor(700 + Math.random() * 250),   // km/h
    heading:     Math.round(heading),
    status:      'IN_FLIGHT',  // IN_FLIGHT | LANDING
    landingTicks: 0,
  }
}

function generateMockFlights(count = 40) {
  return Array.from({ length: count }, (_, i) => {
    const { orig, dest } = pickRoute()
    // 各架飞机从随机进度出发，模拟已在途中
    return buildFlight(i, orig, dest, 0.05 + Math.random() * 0.6)
  })
}

/** 每个仿真 tick 推进一架飞机 */
function stepFlight(f, dtSeconds) {
  if (f.status === 'LANDING') {
    f.landingTicks--
    if (f.landingTicks <= 0) {
      const { orig, dest } = pickRoute()
      const idNum = parseInt(f.flightId.replace('MOCK', '').replace('REAL', ''), 10) || 0
      const fresh = buildFlight(idNum, orig, dest, 0)
      const keepId = f.flightId   // 保留原 ID，保证 markerMap 一直有效
      Object.assign(f, fresh)
      f.flightId = keepId
    }
    return
  }

  const totalKm = haversineKm(f.origLat, f.origLng, f.destLat, f.destLng)
  const distKm  = (f.speed / 3600) * dtSeconds
  f.progress = Math.min(1, f.progress + distKm / totalKm)

  // 严格沿直线插值
  f.latitude  = f.origLat + (f.destLat - f.origLat) * f.progress
  f.longitude = f.origLng + (f.destLng - f.origLng) * f.progress

  if (f.progress >= 0.99) {
    f.status      = 'LANDING'
    f.landingTicks = 20   // ~2.4 秒后重生
  }
}

export const flightStore = reactive({
  flights: [],
  selectedFlight: null,
  loading: false,
  useMock: false,

  /** 搜索关键词（航班号 / 出发地 / 目的地） */
  searchQuery: '',
  /** 触发地图定位的 flightId，FlightMap 监听后平移到该航班并自动清空 */
  focusFlightId: null,

  /** 选中并聚焦到指定航班（同时触发地图平移） */
  focusFlight(flightId) {
    this.selectFlight(flightId)
    this.focusFlightId = flightId
    this.searchQuery = ''
    this.selectedAirport = null   // 关闭机场面板
  },

  /** 当前展开的机场名称 */
  selectedAirport: null,

  selectAirport(name) {
    this.selectedAirport = name
    this.selectedFlight = null    // 关闭航班弹窗
  },

  clearAirport() {
    this.selectedAirport = null
  },

  /** 返回指定机场的出发/到达航班 */
  getAirportFlights(name) {
    const active = this.flights.filter(f => f.status !== 'LANDING')
    return {
      departing: active.filter(f => f.origin === name),
      arriving:  active.filter(f => f.destination === name),
    }
  },

  simSpeed:  1,
  simPaused: false,
  simTick:   0,

  _simTimer:   null,
  _TICK_MS:    120,
  _SIM_BASE_S: 25,

  startSimulation() {
    if (this._simTimer) return
    this._simTimer = setInterval(() => {
      if (this.simPaused || this.simSpeed === 0) return
      const dt = this._SIM_BASE_S * this.simSpeed
      this.flights.forEach(f => stepFlight(f, dt))
      if (this.selectedFlight?.status === 'LANDING') this.selectedFlight = null
      this.simTick++
    }, this._TICK_MS)
  },

  stopSimulation() {
    clearInterval(this._simTimer)
    this._simTimer = null
  },

  setSimSpeed(speed) {
    this.simSpeed  = speed
    this.simPaused = speed === 0
  },

  async loadFlights() {
    this.loading = true
    try {
      if (this.useMock) {
        await new Promise(r => setTimeout(r, 200))
        this.flights = generateMockFlights(40)
      } else {
        const res = await fetchFlights()
        // 用机场名匹配本地坐标，补充仿真引擎所需字段
        this.flights = res.data.map((f, i) => {
          const orig = AIRPORTS.find(a => a.name === f.origin)
            || AIRPORTS[i % AIRPORTS.length]
          const dest = AIRPORTS.find(a => a.name === f.destination)
            || AIRPORTS[(i + 1) % AIRPORTS.length]
          const totalDist = haversineKm(orig.lat, orig.lng, dest.lat, dest.lng)
          const traveledDist = haversineKm(orig.lat, orig.lng, f.latitude, f.longitude)
          const progress = totalDist > 0 ? Math.min(0.95, traveledDist / totalDist) : 0.5
          const heading = ((Math.atan2(dest.lng - orig.lng, dest.lat - orig.lat) * 180) / Math.PI + 360) % 360
          return {
            flightId: `REAL${f.id}`,
            flightNo:    f.flightNo,
            origin:      f.origin,
            destination: f.destination,
            origLat:  orig.lat,
            origLng:  orig.lng,
            destLat:  dest.lat,
            destLng:  dest.lng,
            progress,
            latitude:  f.latitude,
            longitude: f.longitude,
            altitude:  f.altitude,
            speed:     f.speed || 800,
            heading:   Math.round(heading),
            status:    'IN_FLIGHT',
            landingTicks: 0,
          }
        })
      }
    } catch {
      this.flights = generateMockFlights(40)
    } finally {
      this.loading = false
    }
  },

  selectFlight(flightId) {
    const found = this.flights.find(f => f.flightId === flightId)
    if (!found || found.status === 'LANDING') return
    this.selectedFlight = found
  },

  clearSelection() { this.selectedFlight = null },
})
