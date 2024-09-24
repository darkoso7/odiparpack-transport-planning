package com.odiparpack.transport_planning.repository;

import com.odiparpack.transport_planning.model.City;
import com.odiparpack.transport_planning.model.RoadSegment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadSegmentRepository extends JpaRepository<RoadSegment, Long> {
    RoadSegment findByOriginAndDestination(City origin, City destination);
}
