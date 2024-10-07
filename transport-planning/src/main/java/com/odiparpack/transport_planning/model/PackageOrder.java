package com.odiparpack.transport_planning.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Entity
@Data
public class PackageOrder {
    @Id
    private String orderId;
    private int quantity;

    @ManyToOne
    private City origin;

    @ManyToOne
    private City destination;

    @Temporal(TemporalType.TIMESTAMP)
    private Date orderDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date deliveryDeadline;
}
