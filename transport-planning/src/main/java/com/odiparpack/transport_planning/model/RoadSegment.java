package com.odiparpack.transport_planning.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class RoadSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private City origin;
    
    @ManyToOne
    private City destination;
    
    private double distance;
    private double speedLimit;
    private double cost; // For GLS penalization
}
