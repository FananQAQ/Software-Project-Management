package com.flighttrack.dto;

import com.flighttrack.entity.Flight;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FlightDTO {

    private Long id;
    private String flightNo;
    private String origin;
    private String destination;
    private Double latitude;
    private Double longitude;
    private Integer altitude;
    private Integer speed;
    private Integer heading;
    private String status;
    private LocalDateTime updatedAt;

    public static FlightDTO from(Flight f) {
        FlightDTO dto = new FlightDTO();
        dto.setId(f.getId());
        dto.setFlightNo(f.getFlightNo());
        dto.setOrigin(f.getOrigin());
        dto.setDestination(f.getDestination());
        dto.setLatitude(f.getLatitude());
        dto.setLongitude(f.getLongitude());
        dto.setAltitude(f.getAltitude());
        dto.setSpeed(f.getSpeed());
        dto.setHeading(f.getHeading());
        dto.setStatus(f.getStatus() != null ? f.getStatus().name() : null);
        dto.setUpdatedAt(f.getUpdatedAt());
        return dto;
    }
}
