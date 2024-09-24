package com.odiparpack.transport_planning.repository;

import com.odiparpack.transport_planning.model.TransportationPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportationPlanRepository extends JpaRepository<TransportationPlan, Long> {
}
