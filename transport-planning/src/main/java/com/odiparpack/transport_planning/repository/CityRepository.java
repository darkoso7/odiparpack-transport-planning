package com.odiparpack.transport_planning.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.odiparpack.transport_planning.model.City;

public interface CityRepository extends JpaRepository<City, String> {
    City findByProvinceIgnoreCase(String province);
}
