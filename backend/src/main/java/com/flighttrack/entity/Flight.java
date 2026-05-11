package com.flighttrack.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

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

    public Flight() {}

    public Flight(
            Long id,
            String flightNo,
            String origin,
            String destination,
            Double latitude,
            Double longitude,
            Integer altitude,
            Integer speed,
            Integer heading,
            FlightStatus status,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.flightNo = flightNo;
        this.origin = origin;
        this.destination = destination;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.heading = heading;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFlightNo() {
        return flightNo;
    }

    public void setFlightNo(String flightNo) {
        this.flightNo = flightNo;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
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

    public Integer getSpeed() {
        return speed;
    }

    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    public Integer getHeading() {
        return heading;
    }

    public void setHeading(Integer heading) {
        this.heading = heading;
    }

    public FlightStatus getStatus() {
        return status;
    }

    public void setStatus(FlightStatus status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public enum FlightStatus {
        IN_FLIGHT, LANDED, DELAYED, CANCELLED
    }
}
