package com.odiparpack.transport_planning.repository;

import com.odiparpack.transport_planning.model.PackageOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PackageOrderRepository extends JpaRepository<PackageOrder, String> {
}
