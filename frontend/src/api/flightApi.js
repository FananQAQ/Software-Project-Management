import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
})

/**
 * 获取所有当前活跃航班（位置、航向等）
 * 后端接口：GET /api/flights
 */
export function fetchFlights() {
  return http.get('/api/flights')
}

/**
 * 获取单个航班详情
 * 后端接口：GET /api/flights/{flightId}
 */
export function fetchFlightDetail(flightId) {
  return http.get(`/api/flights/${flightId}`)
}

/**
 * 获取航班历史轨迹
 * 后端接口：GET /api/flights/{flightId}/track
 */
export function fetchFlightTrack(flightId) {
  return http.get(`/api/flights/${flightId}/track`)
}
