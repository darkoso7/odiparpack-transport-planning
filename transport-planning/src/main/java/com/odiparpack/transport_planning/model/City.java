package com.odiparpack.transport_planning.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class City {
    @Id
    private String ubigeo;
    private String department;
    private String province;
    private double latitude;
    private double longitude;
    private String region;
    private int warehouseCapacity;
}
