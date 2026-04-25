package com.flighttrack.controller;

import com.flighttrack.dto.FlightDTO;
import com.flighttrack.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    /**
     * 获取所有当前活跃航班
     * GET /api/flights
     */
    @GetMapping
    public ResponseEntity<List<FlightDTO>> getAllFlights() {
        return ResponseEntity.ok(flightService.getInFlightFlights());
    }

    /**
     * 获取单个航班详情
     * GET /api/flights/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<FlightDTO> getFlightById(@PathVariable Long id) {
        return ResponseEntity.ok(flightService.getFlightById(id));
    }

    /**
     * 获取航班历史轨迹
     * GET /api/flights/{id}/track
     */
    @GetMapping("/{id}/track")
    public ResponseEntity<List<Map<String, Double>>> getFlightTrack(@PathVariable Long id) {
        return ResponseEntity.ok(flightService.getFlightTrack(id));
    }
}
