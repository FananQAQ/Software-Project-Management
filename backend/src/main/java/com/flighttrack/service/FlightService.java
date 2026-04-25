package com.flighttrack.service;

import com.flighttrack.dto.FlightDTO;
import com.flighttrack.entity.Flight;
import com.flighttrack.entity.FlightTrackPoint;
import com.flighttrack.repository.FlightRepository;
import com.flighttrack.repository.FlightTrackPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;
    private final FlightTrackPointRepository trackPointRepository;

    /** 获取所有航班（简要信息） */
    public List<FlightDTO> getAllFlights() {
        return flightRepository.findAll()
                .stream()
                .map(FlightDTO::from)
                .toList();
    }

    /** 获取飞行中的航班 */
    public List<FlightDTO> getInFlightFlights() {
        return flightRepository.findByStatus(Flight.FlightStatus.IN_FLIGHT)
                .stream()
                .map(FlightDTO::from)
                .toList();
    }

    /** 根据 ID 获取航班详情 */
    public FlightDTO getFlightById(Long id) {
        return flightRepository.findById(id)
                .map(FlightDTO::from)
                .orElseThrow(() -> new RuntimeException("Flight not found: " + id));
    }

    /** 获取航班历史轨迹 */
    public List<Map<String, Double>> getFlightTrack(Long flightId) {
        return trackPointRepository.findByFlightIdOrderByRecordedAtAsc(flightId)
                .stream()
                .map(p -> Map.of("latitude", p.getLatitude(), "longitude", p.getLongitude()))
                .toList();
    }

    /** 新增或更新航班位置（供数据采集层调用） */
    @Transactional
    public FlightDTO saveOrUpdateFlight(Flight flight) {
        flightRepository.findByFlightNo(flight.getFlightNo())
                .ifPresent(existing -> flight.setId(existing.getId()));
        Flight saved = flightRepository.save(flight);

        // 记录轨迹点
        FlightTrackPoint point = new FlightTrackPoint();
        point.setFlight(saved);
        point.setLatitude(saved.getLatitude());
        point.setLongitude(saved.getLongitude());
        point.setAltitude(saved.getAltitude());
        trackPointRepository.save(point);

        return FlightDTO.from(saved);
    }
}
