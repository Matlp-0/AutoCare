package com.example.myapplication.notification;

import com.example.myapplication.AppContainer;
import com.example.myapplication.data.local.entity.OdometerReading;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.domain.fuel.KmReminderPolicy;
import com.example.myapplication.domain.fuel.UsageEstimator;
import com.example.myapplication.domain.scheduler.MaintenanceScheduler;

import java.util.List;

/**
 * Verificação diária: recalcula o cronograma de cada veículo, persiste o snapshot
 * e avisa o que está vencendo ou com a quilometragem velha.
 *
 * <p>Fica fora do Worker de propósito — quem agenda é detalhe de plataforma, a
 * regra é a mesma se um dia rodar por outro gatilho.
 */
public class ReminderChecker {

    /** Roda a verificação. Bloqueia: só chamar fora da main thread. */
    public void run(AppContainer container, long now) {
        // Todos os veículos da garagem: o carro secundário também vence óleo,
        // mesmo sem estar em foco na tela.
        List<Vehicle> vehicles = container.vehicleRepository.findAllSync();
        boolean named = vehicles.size() > 1;
        for (Vehicle vehicle : vehicles) {
            check(container, vehicle, named ? vehicle.displayName() : null, now);
        }
    }

    private void check(AppContainer container, Vehicle vehicle, String vehicleName, long now) {
        UsageEstimator.Usage usage = container.usageEstimator.estimate(
                container.odometerRepository.findByVehicleSync(vehicle.id), now);

        MaintenanceScheduler.Input input = new MaintenanceScheduler.Input();
        input.vehicle = vehicle;
        input.now = now;
        input.history = container.maintenanceRepository.findByVehicleSync(vehicle.id);
        input.items = container.maintenanceRepository.findItemsByVehicleSync(vehicle.id);
        input.plan = container.planRepository.planForSync(vehicle);
        input.kmPerDay = usage.reliable ? usage.kmPerDay : 0f;

        MaintenanceScheduler.Result result = container.scheduler.calculate(input);
        container.scheduleRepository.persistSync(vehicle.id, result);
        if (!result.overdue.isEmpty()) {
            container.notifier.notifyUpcoming(result.overdue, vehicleName, vehicle.id);
        } else if (!result.dueSoon.isEmpty()) {
            container.notifier.notifyUpcoming(result.dueSoon, vehicleName, vehicle.id);
        }

        remindKmUpdate(container, vehicle, vehicleName, usage, now);
    }

    /**
     * Sem leitura recente do hodômetro, os prazos em km param no tempo: o app
     * segue achando que o carro está na km da última vez que o usuário contou.
     */
    private void remindKmUpdate(AppContainer container, Vehicle vehicle, String vehicleName,
                                UsageEstimator.Usage usage, long now) {
        OdometerReading last = container.odometerRepository.findLastSync(vehicle.id);
        long lastReadingDate = last != null ? last.date : vehicle.updatedAt;

        KmReminderPolicy.Decision decision = container.kmReminderPolicy.decide(lastReadingDate,
                container.preferences.getLastKmReminderAt(vehicle.id),
                usage.reliable ? usage.kmPerDay : 0f, now);
        if (!decision.shouldNotify) {
            return;
        }
        container.notifier.notifyKmUpdate(decision.daysSinceReading, decision.estimatedKmSince,
                vehicleName, vehicle.id);
        container.preferences.setLastKmReminderAt(vehicle.id, now);
    }
}
