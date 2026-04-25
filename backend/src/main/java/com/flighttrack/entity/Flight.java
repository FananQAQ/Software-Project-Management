package com.flighttrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "flights")
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 航班号，如 CA1001 */
    @Column(nullable = false, unique = true)
    private String flightNo;

    /** 出发机场名称 */
    private String origin;

    /** 目的机场名称 */
    private String destination;

    /** 当前纬度 */
    private Double latitude;

    /** 当前经度 */
    private Double longitude;

    /** 飞行高度（米） */
    private Integer altitude;

    /** 飞行速度（km/h） */
    private Integer speed;

    /** 航向角（0-359 度，0 为正北） */
    private Integer heading;

    /** 航班状态：IN_FLIGHT / LANDED / DELAYED / CANCELLED */
    @Enumerated(EnumType.STRING)
    private FlightStatus status;

    /** 数据更新时间 */
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum FlightStatus {
        IN_FLIGHT, LANDED, DELAYED, CANCELLED
    }
}
