import { reactive } from 'vue'
import { fetchFlights, fetchFlightDetail, fetchFlightTrack } from '../api/flightApi'

// 模拟数据：后端未就绪时使用
function generateMockFlights(count = 30) {
  const airlines = ['CA', 'MU', 'CZ', 'HU', 'FM', 'ZH', 'MF', 'SC']
  const flights = []
  for (let i = 0; i < count; i++) {
    flights.push({
      flightId: `MOCK${String(i).padStart(3, '0')}`,
      flightNo: `${airlines[i % airlines.length]}${1000 + i}`,
      latitude: 20 + Math.random() * 30,
      longitude: 100 + Math.random() * 30,
      altitude: Math.floor(3000 + Math.random() * 9000),
      speed: Math.floor(600 + Math.random() * 400),
      heading: Math.floor(Math.random() * 360),
      origin: '出发地',
      destination: '目的地',
      status: 'IN_FLIGHT',
    })
  }
  return flights
}

export const flightStore = reactive({
  flights: [],
  selectedFlight: null,
  selectedTrack: [],
  loading: false,
  useMock: true, // 后端就绪后改为 false

  async loadFlights() {
    this.loading = true
    try {
      if (this.useMock) {
        await new Promise(r => setTimeout(r, 300))
        this.flights = generateMockFlights(35)
      } else {
        const res = await fetchFlights()
        this.flights = res.data
      }
    } catch {
      this.flights = generateMockFlights(35)
    } finally {
      this.loading = false
    }
  },

  async selectFlight(flightId) {
    if (this.useMock) {
      this.selectedFlight = this.flights.find(f => f.flightId === flightId) || null
      this.selectedTrack = this.selectedFlight
        ? Array.from({ length: 8 }, (_, i) => ({
            latitude: this.selectedFlight.latitude - 0.5 + i * 0.15 + (Math.random() - 0.5) * 0.1,
            longitude: this.selectedFlight.longitude - 0.5 + i * 0.12 + (Math.random() - 0.5) * 0.1,
          }))
        : []
      return
    }
    try {
      const [detail, track] = await Promise.all([
        fetchFlightDetail(flightId),
        fetchFlightTrack(flightId),
      ])
      this.selectedFlight = detail.data
      this.selectedTrack = track.data
    } catch {
      // 保持当前选中不变
    }
  },

  clearSelection() {
    this.selectedFlight = null
    this.selectedTrack = []
  },
})
