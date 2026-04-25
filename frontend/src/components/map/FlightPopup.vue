<template>
  <div class="flight-popup">
    <div class="popup-header">
      <span class="flight-no">✈ {{ flight.flightNo }}</span>
      <button class="close-btn" @click="$emit('close')">✕</button>
    </div>
    <div class="popup-body">
      <div class="info-row">
        <span class="label">状态</span>
        <span class="value status">{{ statusText }}</span>
      </div>
      <div class="info-row">
        <span class="label">高度</span>
        <span class="value">{{ flight.altitude }} m</span>
      </div>
      <div class="info-row">
        <span class="label">速度</span>
        <span class="value">{{ flight.speed }} km/h</span>
      </div>
      <div class="info-row">
        <span class="label">航向</span>
        <span class="value">{{ flight.heading }}°</span>
      </div>
      <div class="info-row">
        <span class="label">位置</span>
        <span class="value coord">
          {{ flight.latitude?.toFixed(4) }}°N,
          {{ flight.longitude?.toFixed(4) }}°E
        </span>
      </div>
      <div v-if="flight.origin" class="info-row">
        <span class="label">出发</span>
        <span class="value">{{ flight.origin }}</span>
      </div>
      <div v-if="flight.destination" class="info-row">
        <span class="label">目的</span>
        <span class="value">{{ flight.destination }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  flight: { type: Object, required: true },
})

defineEmits(['close'])

const statusText = computed(() => {
  const map = {
    IN_FLIGHT: '飞行中',
    LANDED: '已落地',
    DELAYED: '延误',
    CANCELLED: '取消',
  }
  return map[props.flight.status] || props.flight.status
})
</script>

<style scoped>
.flight-popup {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 240px;
  background: rgba(10, 14, 26, 0.92);
  border: 1px solid rgba(0, 229, 255, 0.3);
  border-radius: 8px;
  color: #e0e0e0;
  font-size: 13px;
  z-index: 1100;
  box-shadow: 0 4px 20px rgba(0, 229, 255, 0.1);
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.flight-no {
  font-size: 15px;
  font-weight: 600;
  color: #00e5ff;
  letter-spacing: 1px;
}

.close-btn {
  background: none;
  border: none;
  color: #888;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
  line-height: 1;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #fff;
}

.popup-body {
  padding: 10px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.label {
  color: #666;
  font-size: 12px;
}

.value {
  color: #ccc;
  font-weight: 500;
}

.value.status {
  color: #76ff03;
}

.value.coord {
  font-size: 11px;
  color: #999;
}
</style>
