package com.odiparpack.transport_planning.model;

import jakarta.persistence.*;
import java.util.List;
import lombok.Data;

@Entity
@Data
public class TransportationPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private Truck truck;
    
    @ManyToMany
    private List<City> route;
    
    @ManyToMany
    private List<PackageOrder> deliveries;
}
