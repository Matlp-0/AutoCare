package com.example.myapplication.ui.finance;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import com.example.myapplication.AppContainer;
import com.example.myapplication.AutoCareApp;
import com.example.myapplication.data.local.entity.OdometerReading;
import com.example.myapplication.data.local.entity.Refuel;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.data.local.relation.MaintenanceWithItems;
import com.example.myapplication.domain.finance.CostCalculator;
import com.example.myapplication.domain.finance.CostSummary;
import com.example.myapplication.domain.fuel.UsageEstimator;
import com.example.myapplication.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;

/**
 * Junta manutenções, abastecimentos e leituras do veículo ativo e devolve o
 * painel financeiro já calculado.
 */
public class FinanceViewModel extends AndroidViewModel {

    private final AppContainer container;
    private final CostCalculator calculator = new CostCalculator();

    private final MediatorLiveData<CostSummary> summary = new MediatorLiveData<>();
    private final LiveData<Vehicle> vehicleSource;

    private LiveData<List<MaintenanceWithItems>> roomHistory;
    private LiveData<List<Refuel>> roomRefuels;
    private LiveData<List<OdometerReading>> roomReadings;

    private Vehicle vehicle;
    private List<MaintenanceWithItems> history = new ArrayList<>();
    private List<Refuel> refuels = new ArrayList<>();
    private List<OdometerReading> readings = new ArrayList<>();

    public FinanceViewModel(@NonNull Application application) {
        super(application);
        container = AutoCareApp.container(application);
        vehicleSource = container.vehicleRepository.observeActiveVehicle();

        summary.addSource(vehicleSource, new Observer<Vehicle>() {
            @Override
            public void onChanged(Vehicle active) {
                vehicle = active;
                rebindSources(active);
                recompute();
            }
        });
    }

    public LiveData<CostSummary> summary() {
        return summary;
    }

    public LiveData<Vehicle> vehicle() {
        return vehicleSource;
    }

    private void rebindSources(Vehicle active) {
        if (roomHistory != null) {
            summary.removeSource(roomHistory);
            summary.removeSource(roomRefuels);
            summary.removeSource(roomReadings);
            roomHistory = null;
            roomRefuels = null;
            roomReadings = null;
        }
        if (active == null) {
            history = new ArrayList<>();
            refuels = new ArrayList<>();
            readings = new ArrayList<>();
            return;
        }

        roomHistory = container.maintenanceRepository.observeByVehicle(active.id);
        summary.addSource(roomHistory, new Observer<List<MaintenanceWithItems>>() {
            @Override
            public void onChanged(List<MaintenanceWithItems> items) {
                history = items == null ? new ArrayList<MaintenanceWithItems>() : items;
                recompute();
            }
        });

        roomRefuels = container.fuelRepository.observeByVehicle(active.id);
        summary.addSource(roomRefuels, new Observer<List<Refuel>>() {
            @Override
            public void onChanged(List<Refuel> items) {
                refuels = items == null ? new ArrayList<Refuel>() : items;
                recompute();
            }
        });

        roomReadings = container.odometerRepository.observeByVehicle(active.id);
        summary.addSource(roomReadings, new Observer<List<OdometerReading>>() {
            @Override
            public void onChanged(List<OdometerReading> items) {
                readings = items == null ? new ArrayList<OdometerReading>() : items;
                recompute();
            }
        });
    }

    private void recompute() {
        if (vehicle == null) {
            summary.setValue(new CostSummary());
            return;
        }
        final CostCalculator.Input input = new CostCalculator.Input();
        input.now = System.currentTimeMillis();
        input.maintenances = new ArrayList<>(history);
        input.refuels = new ArrayList<>(refuels);
        input.readings = new ArrayList<>(readings);

        AppExecutors.get().computation().execute(new Runnable() {
            @Override
            public void run() {
                UsageEstimator.Usage usage =
                        container.usageEstimator.estimate(input.readings, input.now);
                input.kmPerDay = usage.reliable ? usage.kmPerDay : 0f;
                summary.postValue(calculator.calculate(input));
            }
        });
    }
}
