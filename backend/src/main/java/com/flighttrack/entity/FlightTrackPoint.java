package com.flighttrack.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 航班轨迹历史点：每隔一段时间记录一次位置快照
 */
@Entity
@Table(name = "flight_track_points",
       indexes = @Index(name = "idx_flight_time", columnList = "flight_id, recorded_at"))
public class FlightTrackPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    private Double latitude;
    private Double longitude;
    private Integer altitude;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    public FlightTrackPoint() {}

    @PrePersist
    public void onCreate() {
        this.recordedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Flight getFlight() {
        return flight;
    }

    public void setFlight(Flight flight) {
        this.flight = flight;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getAltitude() {
        return altitude;
    }

    public void setAltitude(Integer altitude) {
        this.altitude = altitude;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
