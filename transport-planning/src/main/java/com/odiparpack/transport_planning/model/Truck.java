package com.odiparpack.transport_planning.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
public class Truck {
    @Id
    private String code; // Código del camión (e.g., A001)

    private String type; // Tipo de camión (A, B, C)
    private int capacity; // Capacidad en unidades

    @ManyToOne
    private City currentLocation; // Ubicación actual del camión

    private boolean isAvailable;

    @Temporal(TemporalType.TIMESTAMP)
    private Date availableFrom;

    // Gestión de averías
    private boolean isBrokenDown;
    private int breakdownType; // 0: Ninguno, 1: Moderado, 2: Grave, 3: Siniestro
    @Temporal(TemporalType.TIMESTAMP)
    private Date breakdownStartTime;
    @Temporal(TemporalType.TIMESTAMP)
    private Date breakdownEndTime;

    // Mantenimiento preventivo
    private boolean isUnderMaintenance;
    @Temporal(TemporalType.TIMESTAMP)
    private Date maintenanceStartTime;
    @Temporal(TemporalType.TIMESTAMP)
    private Date maintenanceEndTime;

    // Añadido para el tiempo de espera en cada ciudad al entregar
    private Date lastStopTime; // Momento en el que el camión entregó el paquete
    private int waitTimeAfterDelivery = 2 * 3600 * 1000; // 2 horas de espera en milisegundos

    public void waitAtCity() {
        if (lastStopTime != null) {
            lastStopTime = new Date(lastStopTime.getTime() + waitTimeAfterDelivery);
        }
    }

    // Verifica si el camión está listo para continuar después de la entrega
    public boolean isReadyToMove(Date currentTime) {
        return currentTime.after(new Date(lastStopTime.getTime() + waitTimeAfterDelivery));
    }

    // Verifica si el camión puede continuar después de mantenimiento o avería
    public boolean isOperational(Date currentSimulationTime) {
        /*
        if (isUnderMaintenance && maintenanceEndTime != null) {
            System.out.println("Truck " + code + " under maintenance until " + maintenanceEndTime);
            return currentSimulationTime.after(maintenanceEndTime);
        }
        if (isBrokenDown && breakdownEndTime != null) {
            System.out.println("Truck " + code + " broken down until " + breakdownEndTime);
            return currentSimulationTime.after(breakdownEndTime);
        }
        */
        return true;
    }
    
}

