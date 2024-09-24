package com.odiparpack.transport_planning.model;

import jakarta.persistence.*;
import java.util.Date;
import lombok.Data;

@Entity
@Data
public class PackageOrder {
    @Id
    private String orderId;
    private int quantity;
    
    @ManyToOne
    private City destination;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date deliveryDeadline;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date orderDate;
}
