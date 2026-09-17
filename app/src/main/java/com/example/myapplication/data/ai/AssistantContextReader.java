package com.example.myapplication.data.ai;

import com.example.myapplication.AppContainer;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.domain.ai.MaintenancePrompt;
import com.example.myapplication.domain.fuel.UsageEstimator;
import com.example.myapplication.domain.scheduler.MaintenanceScheduler;

/** Reads a consistent snapshot for the requested vehicle; never writes to the database. */
public final class AssistantContextReader {
    private final AppContainer container;
    public AssistantContextReader(AppContainer container) { this.container = container; }

    public String prompt(long vehicleId, String question) {
        return container.database.runInTransaction(() -> {
            Vehicle vehicle = container.database.vehicleDao().findById(vehicleId);
            if (vehicle == null) throw new IllegalArgumentException("Veículo não encontrado. Volte e selecione um veículo.");
            MaintenanceScheduler.Input input = new MaintenanceScheduler.Input();
            input.vehicle = vehicle;
            input.history = container.database.maintenanceDao().findByVehicle(vehicleId);
            input.items = container.database.maintenanceItemDao().findByVehicle(vehicleId);
            input.plan = container.planRepository.planForSync(vehicle);
            UsageEstimator.Usage usage = container.usageEstimator.estimate(
                    container.odometerRepository.findByVehicleSync(vehicleId), input.now);
            input.kmPerDay = usage.reliable ? usage.kmPerDay : 0f;
            return MaintenancePrompt.build(vehicle, input.history, container.scheduler.calculate(input),
                    !input.plan.isEmpty(), input.now, question);
        });
    }
}
