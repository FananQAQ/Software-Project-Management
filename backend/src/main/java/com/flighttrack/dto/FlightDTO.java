package com.flighttrack.dto;

import com.flighttrack.entity.Flight;

import java.time.LocalDateTime;

public class FlightDTO {

    private Long id;
    /** OpenSky 航空器地址码；仅实时源返回，数据库航班为空 */
    private String icao24;
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

    /** 与 latitude/longitude 一致的航线起点（度），供前端画虚线；OpenSky 合成航段 */
    private Double origLat;
    private Double origLng;
    private Double destLat;
    private Double destLng;
    /** 当前点在 orig→dest 航段上的比例 [0,1]；OpenSky 与实时位置对齐 */
    private Double routeProgress;

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
        dto.setIcao24(null);
        dto.setOrigLat(null);
        dto.setOrigLng(null);
        dto.setDestLat(null);
        dto.setDestLng(null);
        dto.setRouteProgress(null);
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIcao24() {
        return icao24;
    }

    public void setIcao24(String icao24) {
        this.icao24 = icao24;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Double getOrigLat() {
        return origLat;
    }

    public void setOrigLat(Double origLat) {
        this.origLat = origLat;
    }

    public Double getOrigLng() {
        return origLng;
    }

    public void setOrigLng(Double origLng) {
        this.origLng = origLng;
    }

    public Double getDestLat() {
        return destLat;
    }

    public void setDestLat(Double destLat) {
        this.destLat = destLat;
    }

    public Double getDestLng() {
        return destLng;
    }

    public void setDestLng(Double destLng) {
        this.destLng = destLng;
    }

    public Double getRouteProgress() {
        return routeProgress;
    }

    public void setRouteProgress(Double routeProgress) {
        this.routeProgress = routeProgress;
    }
}
