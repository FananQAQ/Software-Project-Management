package com.flighttrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flighttrack.dto.FlightDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 从 OpenSky Network 拉取全量 state vectors，按 {@code origin_country} 分组，
 * 每个国家随机最多保留 {@code maxPerCountry} 条在空且有位置信息的航空器（默认 50）。
 *
 * @see <a href="https://openskynetwork.github.io/opensky-api/rest.html">OpenSky REST</a>
 */
@Service
public class OpenSkyIngestService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double KM_BEHIND = 150.0;
    private static final double KM_AHEAD = 300.0;

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String statesUrl;
    private final int maxPerCountry;
    private final int maxPerGridCell;
    private final int gridDeg;
    private final String basicAuthHeader;

    public OpenSkyIngestService(
            ObjectMapper objectMapper,
            @Value("${opensky.states-url:https://opensky-network.org/api/states/all?lamin=18&lamax=53&lomin=73&lomax=135}") String statesUrl,
            @Value("${opensky.max-per-country:50}") int maxPerCountry,
            @Value("${opensky.max-per-grid-cell:20}") int maxPerGridCell,
            @Value("${opensky.grid-deg:10}") int gridDeg,
            @Value("${opensky.username:}") String username,
            @Value("${opensky.password:}") String password
    ) {
        this.objectMapper = objectMapper;
        this.statesUrl = statesUrl;
        this.maxPerCountry = maxPerCountry;
        this.maxPerGridCell = maxPerGridCell;
        this.gridDeg = Math.max(1, gridDeg);
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(15_000);
        rf.setReadTimeout(120_000);
        this.restClient = RestClient.builder().requestFactory(rf).build();
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            String token = Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(StandardCharsets.UTF_8));
            this.basicAuthHeader = "Basic " + token;
        } else {
            this.basicAuthHeader = null;
        }
    }

    public List<FlightDTO> fetchSampledFlights() {
        try {
            String json = fetchStatesJson();
            JsonNode root = objectMapper.readTree(json);
            JsonNode states = root.get("states");
            if (states == null || !states.isArray()) {
                return List.of();
            }

            Map<String, List<Candidate>> byCell = new HashMap<>();
            for (JsonNode row : states) {
                if (row == null || row.isNull() || !row.isArray() || row.size() < 11) {
                    continue;
                }
                if (truthyOnGround(row)) {
                    continue;
                }
                Double lat = readDouble(row.get(6));
                Double lon = readDouble(row.get(5));
                if (lat == null || lon == null) {
                    continue;
                }
                String country = textOrEmpty(row.get(2));
                if (!StringUtils.hasText(country)) {
                    country = "Unknown";
                }
                String cell = gridCellKey(lat, lon);
                byCell.computeIfAbsent(cell, k -> new ArrayList<>()).add(new Candidate(row, country));
            }

            // 先保证「每个地理网格都有机会被抽到」，避免都挤在同一片区域
            List<String> cells = new ArrayList<>(byCell.keySet());
            Collections.shuffle(cells, ThreadLocalRandom.current());
            for (String cell : cells) {
                Collections.shuffle(byCell.get(cell), ThreadLocalRandom.current());
            }

            Map<String, Integer> countryCount = new HashMap<>();
            Map<String, Boolean> seen = new HashMap<>();
            List<FlightDTO> out = new ArrayList<>();

            for (String cell : cells) {
                int pickedInCell = 0;
                for (Candidate c : byCell.get(cell)) {
                    if (pickedInCell >= maxPerGridCell) break;
                    int used = countryCount.getOrDefault(c.country(), 0);
                    if (used >= maxPerCountry) continue;
                    String icao24 = textOrEmpty(c.row().get(0));
                    if (StringUtils.hasText(icao24) && seen.putIfAbsent(icao24, true) != null) {
                        continue;
                    }
                    FlightDTO dto = toDto(c.row());
                    if (dto == null) continue;
                    out.add(dto);
                    countryCount.put(c.country(), used + 1);
                    pickedInCell++;
                }
            }

            return out;
        } catch (Exception e) {
            throw new IllegalStateException("OpenSky failed: " + e.getMessage(), e);
        }
    }

    private record Candidate(JsonNode row, String country) {}

    private String gridCellKey(double lat, double lon) {
        // 以 gridDeg 度为边长的网格（例如 10°×10°），将同区域的飞机归为一桶
        int latKey = (int) Math.floor(lat / gridDeg);
        int lonKey = (int) Math.floor(lon / gridDeg);
        return latKey + ":" + lonKey;
    }

    private String fetchStatesJson() {
        if (basicAuthHeader != null) {
            return restClient.get()
                    .uri(statesUrl)
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
                    .retrieve()
                    .body(String.class);
        }
        return restClient.get()
                .uri(statesUrl)
                .retrieve()
                .body(String.class);
    }

    private static boolean truthyOnGround(JsonNode row) {
        JsonNode n = row.get(8);
        return n != null && !n.isNull() && n.isBoolean() && n.booleanValue();
    }

    private FlightDTO toDto(JsonNode row) {
        String icao24 = textOrEmpty(row.get(0));
        if (!StringUtils.hasText(icao24)) {
            return null;
        }
        String callsign = trimCallsign(row.get(1));
        String country = textOrEmpty(row.get(2));
        if (!StringUtils.hasText(country)) {
            country = "Unknown";
        }

        Double lon = readDouble(row.get(5));
        Double lat = readDouble(row.get(6));
        if (lat == null || lon == null) {
            return null;
        }

        Double baro = readDouble(row.get(7));
        Double geo = row.size() > 13 ? readDouble(row.get(13)) : null;
        int altitudeM = (int) Math.round(baro != null ? baro : (geo != null ? geo : 10_000.0));

        Double velocityMs = readDouble(row.get(9));
        int speedKmh = velocityMs != null && velocityMs > 0
                ? (int) Math.round(velocityMs * 3.6)
                : 800;

        Double track = readDouble(row.get(10));
        double headingDeg = track != null ? track : 0.0;

        double[] behind = offsetLatLon(lat, lon, headingDeg + 180.0, KM_BEHIND);
        double[] ahead = offsetLatLon(lat, lon, headingDeg, KM_AHEAD);
        double origLat = behind[0];
        double origLng = behind[1];
        double destLat = ahead[0];
        double destLng = ahead[1];
        double progress = KM_BEHIND / (KM_BEHIND + KM_AHEAD);

        FlightDTO dto = new FlightDTO();
        dto.setIcao24(icao24);
        dto.setFlightNo(StringUtils.hasText(callsign) ? callsign : icao24.toUpperCase());
        dto.setOrigin(country);
        dto.setDestination("OpenSky · 沿航迹");
        dto.setLatitude(lat);
        dto.setLongitude(lon);
        dto.setAltitude(altitudeM);
        dto.setSpeed(speedKmh);
        dto.setHeading(Math.floorMod(Math.round((float) headingDeg), 360));
        dto.setStatus("IN_FLIGHT");
        dto.setUpdatedAt(LocalDateTime.now());
        dto.setOrigLat(origLat);
        dto.setOrigLng(origLng);
        dto.setDestLat(destLat);
        dto.setDestLng(destLng);
        dto.setRouteProgress(progress);
        return dto;
    }

    /**
     * 从 (lat, lon) 沿真航向 trackFromNorthDeg（度，从正北顺时针）移动 distanceKm。
     */
    static double[] offsetLatLon(double latDeg, double lonDeg, double trackFromNorthDeg, double distanceKm) {
        double brng = Math.toRadians(trackFromNorthDeg);
        double lat1 = Math.toRadians(latDeg);
        double lon1 = Math.toRadians(lonDeg);
        double ang = distanceKm / EARTH_RADIUS_KM;
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(ang) + Math.cos(lat1) * Math.sin(ang) * Math.cos(brng));
        double lon2 = lon1 + Math.atan2(
                Math.sin(brng) * Math.sin(ang) * Math.cos(lat1),
                Math.cos(ang) - Math.sin(lat1) * Math.sin(lat2));
        return new double[]{Math.toDegrees(lat2), Math.toDegrees(lon2)};
    }

    private static String trimCallsign(JsonNode n) {
        if (n == null || n.isNull() || !n.isTextual()) {
            return "";
        }
        return n.asText().trim();
    }

    private static String textOrEmpty(JsonNode n) {
        if (n == null || n.isNull()) {
            return "";
        }
        if (n.isTextual()) {
            return n.asText().trim();
        }
        return n.asText();
    }

    private static Double readDouble(JsonNode n) {
        if (n == null || n.isNull() || !n.isNumber()) {
            return null;
        }
        return n.asDouble();
    }
}
