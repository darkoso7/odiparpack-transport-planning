package com.odiparpack.transport_planning.repository;

import com.odiparpack.transport_planning.model.Truck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TruckRepository extends JpaRepository<Truck, String> {
}
