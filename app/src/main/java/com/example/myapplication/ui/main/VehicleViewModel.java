package com.example.myapplication.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import com.example.myapplication.AppContainer;
import com.example.myapplication.AutoCareApp;
import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.data.local.entity.MaintenanceItem;
import com.example.myapplication.data.local.entity.OdometerReading;
import com.example.myapplication.data.local.entity.Refuel;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.data.local.relation.MaintenanceWithItems;
import com.example.myapplication.domain.fuel.UsageEstimator;
import com.example.myapplication.domain.manual.ManualPlanEntry;
import com.example.myapplication.domain.model.DashboardState;
import com.example.myapplication.domain.scheduler.MaintenanceScheduler;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.Callback;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel compartilhado pelas abas (Início, Revisões, Meu carro e Manutenções).
 * Toda regra de negócio fica no domínio; aqui só orquestramos dados e threads.
 */
public class VehicleViewModel extends AndroidViewModel {

    private final AppContainer container;

    private final MediatorLiveData<DashboardState> dashboard = new MediatorLiveData<>();

    private final LiveData<Vehicle> vehicleSource;
    /**
     * Histórico do veículo ativo. É um mediator próprio para continuar recebendo
     * updates do Room mesmo quando só a aba Manutenções está na tela.
     */
    private final MediatorLiveData<List<MaintenanceWithItems>> historySource = new MediatorLiveData<>();
    private LiveData<List<MaintenanceWithItems>> roomHistory;

    /** Abastecimentos do veículo ativo; alimentam a aba Combustível e o painel. */
    private final MediatorLiveData<List<Refuel>> refuelSource = new MediatorLiveData<>();
    private LiveData<List<Refuel>> roomRefuels;

    private Vehicle currentVehicle;
    private List<MaintenanceWithItems> currentHistory = new ArrayList<>();
    private List<Refuel> currentRefuels = new ArrayList<>();

    public VehicleViewModel(@NonNull Application application) {
        super(application);
        container = AutoCareApp.container(application);

        vehicleSource = container.vehicleRepository.observeActiveVehicle();

        // O histórico segue o veículo ativo; o dashboard depende do histórico.
        // Assim qualquer uma das abas mantém a cadeia do Room ativa sozinha.
        historySource.addSource(vehicleSource, new Observer<Vehicle>() {
            @Override
            public void onChanged(Vehicle vehicle) {
                bindHistory(vehicle);
            }
        });
        refuelSource.addSource(vehicleSource, new Observer<Vehicle>() {
            @Override
            public void onChanged(Vehicle vehicle) {
                bindRefuels(vehicle);
            }
        });

        dashboard.addSource(vehicleSource, new Observer<Vehicle>() {
            @Override
            public void onChanged(Vehicle vehicle) {
                currentVehicle = vehicle;
                recompute();
            }
        });
        dashboard.addSource(historySource, new Observer<List<MaintenanceWithItems>>() {
            @Override
            public void onChanged(List<MaintenanceWithItems> items) {
                currentHistory = items == null ? new ArrayList<MaintenanceWithItems>() : items;
                recompute();
            }
        });
        dashboard.addSource(refuelSource, new Observer<List<Refuel>>() {
            @Override
            public void onChanged(List<Refuel> refuels) {
                currentRefuels = refuels == null ? new ArrayList<Refuel>() : refuels;
                recompute();
            }
        });
    }

    private void bindHistory(Vehicle vehicle) {
        if (roomHistory != null) {
            historySource.removeSource(roomHistory);
            roomHistory = null;
        }
        if (vehicle == null) {
            historySource.setValue(new ArrayList<MaintenanceWithItems>());
            return;
        }
        roomHistory = container.maintenanceRepository.observeByVehicle(vehicle.id);
        historySource.addSource(roomHistory, new Observer<List<MaintenanceWithItems>>() {
            @Override
            public void onChanged(List<MaintenanceWithItems> items) {
                historySource.setValue(items == null
                        ? new ArrayList<MaintenanceWithItems>() : items);
            }
        });
    }

    private void bindRefuels(Vehicle vehicle) {
        if (roomRefuels != null) {
            refuelSource.removeSource(roomRefuels);
            roomRefuels = null;
        }
        if (vehicle == null) {
            refuelSource.setValue(new ArrayList<Refuel>());
            return;
        }
        roomRefuels = container.fuelRepository.observeByVehicle(vehicle.id);
        refuelSource.addSource(roomRefuels, new Observer<List<Refuel>>() {
            @Override
            public void onChanged(List<Refuel> refuels) {
                refuelSource.setValue(refuels == null ? new ArrayList<Refuel>() : refuels);
            }
        });
    }

    public LiveData<DashboardState> dashboard() {
        return dashboard;
    }

    public LiveData<Vehicle> vehicle() {
        return vehicleSource;
    }

    /** Todos os veículos cadastrados, para a barra lateral. */
    public LiveData<List<Vehicle>> vehicles() {
        return container.vehicleRepository.observeAll();
    }

    /** Troca o veículo em foco; histórico, painel e abastecimentos seguem junto. */
    public void selectVehicle(long vehicleId) {
        container.vehicleRepository.setActiveVehicle(vehicleId);
    }

    /** Grava a ordem da garagem definida por arrasto. */
    public void reorderVehicles(List<Long> orderedIds) {
        container.vehicleRepository.reorder(orderedIds, null);
    }

    public void deleteVehicle(long vehicleId, Callback<Boolean> callback) {
        container.vehicleRepository.delete(vehicleId, callback);
    }

    public LiveData<List<MaintenanceWithItems>> history() {
        return historySource;
    }

    public LiveData<List<Refuel>> refuels() {
        return refuelSource;
    }

    /** Recalcula o painel fora da main thread e persiste o snapshot do cronograma. */
    private void recompute() {
        final Vehicle vehicle = currentVehicle;
        final List<MaintenanceWithItems> historySnapshot = new ArrayList<>(currentHistory);
        final List<Refuel> refuelSnapshot = new ArrayList<>(currentRefuels);
        if (vehicle == null) {
            dashboard.setValue(new DashboardState());
            return;
        }
        AppExecutors.get().computation().execute(new Runnable() {
            @Override
            public void run() {
                DashboardState state = buildState(vehicle, historySnapshot, refuelSnapshot);
                dashboard.postValue(state);
            }
        });
    }

    private DashboardState buildState(Vehicle vehicle, List<MaintenanceWithItems> historySnapshot,
                                      List<Refuel> refuelSnapshot) {
        List<Maintenance> maintenances = new ArrayList<>();
        List<MaintenanceItem> items = new ArrayList<>();
        double totalSpent = 0d;
        for (MaintenanceWithItems entry : historySnapshot) {
            maintenances.add(entry.maintenance);
            items.addAll(entry.items);
            totalSpent += entry.maintenance.totalCost;
        }

        List<ManualPlanEntry> plan = container.planRepository.planForSync(vehicle);

        long now = System.currentTimeMillis();
        List<OdometerReading> readings =
                container.odometerRepository.findByVehicleSync(vehicle.id);
        UsageEstimator.Usage usage = container.usageEstimator.estimate(readings, now);

        MaintenanceScheduler.Input input = new MaintenanceScheduler.Input();
        input.vehicle = vehicle;
        input.now = now;
        input.history = maintenances;
        input.items = items;
        input.plan = plan;
        input.kmPerDay = usage.reliable ? usage.kmPerDay : 0f;

        MaintenanceScheduler.Result result = container.scheduler.calculate(input);

        DashboardState state = new DashboardState();
        state.vehicle = vehicle;
        state.schedule = result;
        state.timeline = container.timelineBuilder.build(vehicle.currentKm, plan, maintenances,
                container.scheduleRepository.findChecksSync(vehicle.id));
        state.maintenanceCount = maintenances.size();
        state.totalSpent = totalSpent;
        state.health = container.healthCalculator.calculate(result, maintenances.size());
        state.lastMaintenance = maintenances.isEmpty() ? null : maintenances.get(0);
        state.usage = usage;
        state.fuel = container.fuelStatsCalculator.calculate(refuelSnapshot, now);

        container.scheduleRepository.persistSync(vehicle.id, result);
        return state;
    }

    /** Recalcula o painel depois que o plano do fabricante muda (busca online). */
    public void refreshPlan() {
        recompute();
    }

    /** Marca/desmarca uma revisão do cronograma como realizada. */
    public void setRevisionDone(int km, boolean done) {
        if (currentVehicle == null) {
            return;
        }
        container.scheduleRepository.setRevisionDone(currentVehicle.id, km, done,
                new Callback<Boolean>() {
                    @Override
                    public void onResult(Boolean result) {
                        recompute();
                    }
                });
    }

    public void updateKm(int km) {
        if (currentVehicle == null) {
            return;
        }
        container.vehicleRepository.updateKm(currentVehicle.id, km, null);
    }
}
