package com.odiparpack.transport_planning.model;

import java.util.List;

public class ShortestPathResult {
    private double cost;
    private List<City> path;

    public ShortestPathResult(double cost, List<City> path) {
        this.cost = cost;
        this.path = path;
    }

    public double getCost() {
        return cost;
    }

    public List<City> getPath() {
        return path;
    }
}
