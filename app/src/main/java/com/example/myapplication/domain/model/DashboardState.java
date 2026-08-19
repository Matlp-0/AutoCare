package com.example.myapplication.domain.model;

import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.domain.fuel.FuelStats;
import com.example.myapplication.domain.fuel.UsageEstimator;
import com.example.myapplication.domain.scheduler.MaintenanceScheduler;

import java.util.ArrayList;
import java.util.List;

/** Estado consolidado usado pela Home, Revisões e Meu carro. */
public class DashboardState {

    public Vehicle vehicle;
    public MaintenanceScheduler.Result schedule = new MaintenanceScheduler.Result();
    public List<RevisionMilestone> timeline = new ArrayList<>();
    public VehicleHealth health = new VehicleHealth(0, "");
    public int maintenanceCount;
    public double totalSpent;
    public Maintenance lastMaintenance;
    public FuelStats fuel = new FuelStats();
    public UsageEstimator.Usage usage = new UsageEstimator.Usage();

    public MaintenanceStatus status() {
        return schedule.status;
    }
}
