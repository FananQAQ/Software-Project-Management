package com.flighttrack.repository;

import com.flighttrack.entity.FlightTrackPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightTrackPointRepository extends JpaRepository<FlightTrackPoint, Long> {

    List<FlightTrackPoint> findByFlightIdOrderByRecordedAtAsc(Long flightId);
}
