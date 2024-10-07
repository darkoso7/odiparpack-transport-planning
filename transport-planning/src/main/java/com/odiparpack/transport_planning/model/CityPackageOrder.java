package com.odiparpack.transport_planning.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Entity
@Data
public class CityPackageOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private City city;

    @ManyToOne
    private PackageOrder packageOrder;

    @Temporal(TemporalType.TIMESTAMP)
    private Date releaseTime;  // Fecha y hora de liberación de capacidad
}
