import { reactive } from 'vue'
import { fetchFlights, fetchFlightDetail, fetchFlightTrack } from '../api/flightApi'

// 国内主要机场
const AIRPORTS = [
  { name: '北京首都', lat: 40.08, lng: 116.58 },
  { name: '上海浦东', lat: 31.14, lng: 121.80 },
  { name: '广州白云', lat: 23.39, lng: 113.30 },
  { name: '深圳宝安', lat: 22.64, lng: 113.81 },
  { name: '成都天府', lat: 30.32, lng: 104.44 },
  { name: '重庆江北', lat: 29.72, lng: 106.64 },
  { name: '昆明长水', lat: 25.10, lng: 102.93 },
  { name: '西安咸阳', lat: 34.44, lng: 108.75 },
  { name: '武汉天河', lat: 30.78, lng: 114.21 },
  { name: '杭州萧山', lat: 30.23, lng: 120.43 },
  { name: '南京禄口', lat: 31.74, lng: 118.86 },
  { name: '郑州新郑', lat: 34.52, lng: 113.84 },
  { name: '长沙黄花', lat: 28.19, lng: 113.22 },
  { name: '青岛胶东', lat: 36.36, lng: 120.08 },
  { name: '厦门高崎', lat: 24.54, lng: 118.13 },
  { name: '哈尔滨太平', lat: 45.62, lng: 126.25 },
  { name: '乌鲁木齐地窝堡', lat: 43.91, lng: 87.47 },
  { name: '拉萨贡嘎', lat: 29.30, lng: 90.91 },
  { name: '海口美兰', lat: 19.94, lng: 110.46 },
  { name: '三亚凤凰', lat: 18.30, lng: 109.41 },
]
const AIRLINES = ['CA', 'MU', 'CZ', 'HU', 'FM', 'ZH', 'MF', 'SC', '3U', 'GS', 'KN', 'BK']

// 根据当前位置和航向生成初始历史轨迹
function generateTrack(flight, points = 12) {
  const rad = (flight.heading * Math.PI) / 180
  const track = []
  for (let i = points; i >= 0; i--) {
    track.push({
      lat: flight.latitude - Math.cos(rad) * i * 0.28 + (Math.random() - 0.5) * 0.04,
      lng: flight.longitude - Math.sin(rad) * i * 0.22 + (Math.random() - 0.5) * 0.04,
    })
  }
  return track
}

function generateMockFlights(count = 40) {
  const flights = []
  for (let i = 0; i < count; i++) {
    const orig = AIRPORTS[i % AIRPORTS.length]
    const dest = AIRPORTS[(i + Math.floor(AIRPORTS.length / 2) + 1) % AIRPORTS.length]
    const progress = 0.1 + Math.random() * 0.8
    const lat = orig.lat + (dest.lat - orig.lat) * progress + (Math.random() - 0.5) * 0.3
    const lng = orig.lng + (dest.lng - orig.lng) * progress + (Math.random() - 0.5) * 0.3
    const dLat = dest.lat - orig.lat
    const dLng = dest.lng - orig.lng
    const heading = ((Math.atan2(dLng, dLat) * 180) / Math.PI + 360) % 360
    const f = {
      flightId: `MOCK${String(i).padStart(3, '0')}`,
      flightNo: `${AIRLINES[i % AIRLINES.length]}${1000 + i}`,
      latitude: Math.max(18.5, Math.min(52, lat)),
      longitude: Math.max(74, Math.min(134, lng)),
      altitude: Math.floor(6000 + Math.random() * 5000),
      speed: Math.floor(700 + Math.random() * 250),  // km/h
      heading: Math.round(heading),
      origin: orig.name,
      destination: dest.name,
      status: 'IN_FLIGHT',
    }
    f.track = generateTrack(f)
    flights.push(f)
  }
  return flights
}

// 将单架飞机按物理规律推进 dtSeconds 仿真秒
function stepFlight(f, dtSeconds) {
  const headingRad = (f.heading * Math.PI) / 180
  const distKm = (f.speed / 3600) * dtSeconds
  const dLat = (distKm * Math.cos(headingRad)) / 111.32
  const dLng = (distKm * Math.sin(headingRad)) / (111.32 * Math.cos((f.latitude * Math.PI) / 180))

  f.latitude += dLat
  f.longitude += dLng

  // 碰到国内边界后反弹
  if (f.latitude < 18.5 || f.latitude > 52) {
    f.heading = (360 - f.heading + 360) % 360
    f.latitude = Math.max(18.5, Math.min(52, f.latitude))
  }
  if (f.longitude < 74 || f.longitude > 134) {
    f.heading = (540 - f.heading + 360) % 360
    f.longitude = Math.max(74, Math.min(134, f.longitude))
  }

  // 追加轨迹点，保留最近 20 个
  f.track.push({ lat: f.latitude, lng: f.longitude })
  if (f.track.length > 20) f.track.shift()
}

export const flightStore = reactive({
  flights: [],
  selectedFlight: null,
  loading: false,
  useMock: true,

  // 仿真控制
  simSpeed: 1,      // 当前倍速 (0 = 暂停)
  simPaused: false,
  simTick: 0,       // 每次推进后递增，供组件监听

  _simTimer: null,
  _TICK_MS: 120,    // 真实世界每帧间隔(ms)
  _SIM_BASE_S: 30,  // 1× 时每帧推进的仿真秒数

  startSimulation() {
    if (this._simTimer) return
    this._simTimer = setInterval(() => {
      if (this.simPaused || this.simSpeed === 0) return
      const dtSeconds = this._SIM_BASE_S * this.simSpeed
      this.flights.forEach(f => stepFlight(f, dtSeconds))
      this.simTick++
    }, this._TICK_MS)
  },

  stopSimulation() {
    clearInterval(this._simTimer)
    this._simTimer = null
  },

  setSimSpeed(speed) {
    this.simSpeed = speed
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
        this.flights = res.data
      }
    } catch {
      this.flights = generateMockFlights(40)
    } finally {
      this.loading = false
    }
  },

  async selectFlight(flightId) {
    const found = this.flights.find(f => f.flightId === flightId)
    if (!found) return
    if (this.useMock) { this.selectedFlight = found; return }
    try {
      const [detail, track] = await Promise.all([
        fetchFlightDetail(flightId),
        fetchFlightTrack(flightId),
      ])
      const updated = { ...detail.data, track: track.data }
      const idx = this.flights.findIndex(f => f.flightId === flightId)
      if (idx !== -1) this.flights[idx] = updated
      this.selectedFlight = updated
    } catch {
      this.selectedFlight = found
    }
  },

  clearSelection() {
    this.selectedFlight = null
  },
})
