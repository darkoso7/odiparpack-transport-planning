package com.odiparpack.transport_planning.model;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Data;

@Entity
@Data
public class Truck {
    @Id
    private String code; // Truck code (e.g., A001)

    private String type; // Type of truck (A, B, C)
    private int capacity; // Capacity in units

    @ManyToOne
    private City currentLocation; // Current city

    private boolean isAvailable;

    @Temporal(TemporalType.TIMESTAMP)
    private Date availableFrom;

    // Additional fields for breakdowns and maintenance (if not already added)
    private boolean isBrokenDown;
    private int breakdownType; // 0: None, 1: Moderate, 2: Severe, 3: Accident
    @Temporal(TemporalType.TIMESTAMP)
    private Date breakdownStartTime;
    @Temporal(TemporalType.TIMESTAMP)
    private Date breakdownEndTime;

    private boolean isUnderMaintenance;
    @Temporal(TemporalType.TIMESTAMP)
    private Date maintenanceStartTime;
    @Temporal(TemporalType.TIMESTAMP)
    private Date maintenanceEndTime;
}

