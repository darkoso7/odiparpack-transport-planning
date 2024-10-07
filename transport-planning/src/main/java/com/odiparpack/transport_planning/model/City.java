package com.odiparpack.transport_planning.model;

import lombok.Data;

import java.util.Date;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL)
    private List<CityPackageOrder> packageOrders = new ArrayList<>();

    public City() {}

    public void addPackageOrder(PackageOrder pkg, Date releaseTime) {
        CityPackageOrder cityPackageOrder = new CityPackageOrder();
        cityPackageOrder.setCity(this);
        cityPackageOrder.setPackageOrder(pkg);
        cityPackageOrder.setReleaseTime(releaseTime);
        packageOrders.add(cityPackageOrder);
    }

    public void releaseCapacity(Date currentSimulationTime) {
        packageOrders.removeIf(order -> order.getReleaseTime().before(currentSimulationTime));
    }

    public int getCurrentCapacity() {
        return packageOrders.stream().mapToInt(order -> order.getPackageOrder().getQuantity()).sum();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        City city = (City) o;
        return ubigeo.equals(city.ubigeo);  // Compare based on unique ubigeo
    }

    @Override
    public int hashCode() {
        return Objects.hash(ubigeo);  // Hash based on unique ubigeo
    }
}
