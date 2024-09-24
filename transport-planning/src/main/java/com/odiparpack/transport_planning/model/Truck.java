package com.odiparpack.transport_planning.model;

import jakarta.persistence.*;
import java.util.Date;

import lombok.Data;

@Entity
@Data
public class Truck {
    @Id
    private String code;
    private String type;
    private int capacity;
    
    @ManyToOne
    private City currentLocation;
    
    private boolean isAvailable;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date availableFrom;
}
