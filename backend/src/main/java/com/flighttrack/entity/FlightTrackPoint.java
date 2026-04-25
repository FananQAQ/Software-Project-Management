package com.flighttrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 航班轨迹历史点：每隔一段时间记录一次位置快照
 */
@Data
@NoArgsConstructor
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

    @PrePersist
    public void onCreate() {
        this.recordedAt = LocalDateTime.now();
    }
}
