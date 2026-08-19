package com.example.myapplication.data.repository;

import androidx.lifecycle.LiveData;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.entity.Document;
import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.data.local.entity.MaintenanceItem;
import com.example.myapplication.data.local.entity.OdometerReading;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.data.local.relation.MaintenanceWithItems;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.Callback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class MaintenanceRepository {

    private final AppDatabase database;
    private final AppExecutors executors = AppExecutors.get();

    public MaintenanceRepository(AppDatabase database) {
        this.database = database;
    }

    public LiveData<List<MaintenanceWithItems>> observeByVehicle(long vehicleId) {
        return database.maintenanceDao().observeByVehicle(vehicleId);
    }

    public LiveData<MaintenanceWithItems> observeById(long id) {
        return database.maintenanceDao().observeById(id);
    }

    public List<Maintenance> findByVehicleSync(long vehicleId) {
        return database.maintenanceDao().findByVehicle(vehicleId);
    }

    public List<MaintenanceItem> findItemsByVehicleSync(long vehicleId) {
        return database.maintenanceItemDao().findByVehicle(vehicleId);
    }

    /**
     * Grava manutenção + peças + documento em uma transação e atualiza a
     * quilometragem do veículo quando o hodômetro informado for maior.
     */
    public void save(final Maintenance maintenance, final List<MaintenanceItem> items,
                     final Document document, final Callback<Long> callback) {
        executors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final long id = database.runInTransaction(new Callable<Long>() {
                    @Override
                    public Long call() {
                        maintenance.createdAt = System.currentTimeMillis();
                        long maintenanceId = database.maintenanceDao().insert(maintenance);

                        List<MaintenanceItem> toInsert = new ArrayList<>();
                        if (items != null) {
                            for (MaintenanceItem item : items) {
                                item.maintenanceId = maintenanceId;
                                toInsert.add(item);
                            }
                        }
                        if (!toInsert.isEmpty()) {
                            database.maintenanceItemDao().insertAll(toInsert);
                        }
                        if (document != null) {
                            document.maintenanceId = maintenanceId;
                            document.createdAt = System.currentTimeMillis();
                            database.documentDao().insert(document);
                        }
                        if (maintenance.odometerKm > 0) {
                            database.odometerReadingDao().insert(OdometerReading.of(
                                    maintenance.vehicleId, maintenance.date,
                                    maintenance.odometerKm,
                                    OdometerReading.SOURCE_MAINTENANCE));
                        }
                        Vehicle vehicle = database.vehicleDao().findById(maintenance.vehicleId);
                        if (vehicle != null && maintenance.odometerKm > vehicle.currentKm) {
                            database.vehicleDao().updateKm(vehicle.id, maintenance.odometerKm,
                                    System.currentTimeMillis());
                        }
                        return maintenanceId;
                    }
                });
                if (callback != null) {
                    executors.mainThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(id);
                        }
                    });
                }
            }
        });
    }

    /**
     * Atualiza uma manutenção existente. Quando {@code items} é nulo, as peças
     * já gravadas são preservadas (o formulário manual não edita peças).
     */
    public void update(final Maintenance maintenance, final List<MaintenanceItem> items,
                       final Callback<Long> callback) {
        executors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                database.runInTransaction(new Runnable() {
                    @Override
                    public void run() {
                        database.maintenanceDao().update(maintenance);
                        if (items != null) {
                            database.maintenanceItemDao().deleteByMaintenance(maintenance.id);
                            List<MaintenanceItem> toInsert = new ArrayList<>();
                            for (MaintenanceItem item : items) {
                                item.maintenanceId = maintenance.id;
                                toInsert.add(item);
                            }
                            if (!toInsert.isEmpty()) {
                                database.maintenanceItemDao().insertAll(toInsert);
                            }
                        }
                        if (maintenance.odometerKm > 0) {
                            database.odometerReadingDao().insert(OdometerReading.of(
                                    maintenance.vehicleId, maintenance.date,
                                    maintenance.odometerKm,
                                    OdometerReading.SOURCE_MAINTENANCE));
                        }
                        Vehicle vehicle = database.vehicleDao().findById(maintenance.vehicleId);
                        if (vehicle != null && maintenance.odometerKm > vehicle.currentKm) {
                            database.vehicleDao().updateKm(vehicle.id, maintenance.odometerKm,
                                    System.currentTimeMillis());
                        }
                    }
                });
                if (callback != null) {
                    executors.mainThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(maintenance.id);
                        }
                    });
                }
            }
        });
    }

    /** Remove a manutenção; peças e documentos saem junto (FK em CASCADE). */
    public void deleteById(final long maintenanceId, final Callback<Boolean> callback) {
        executors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                database.maintenanceDao().deleteById(maintenanceId);
                if (callback != null) {
                    executors.mainThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(true);
                        }
                    });
                }
            }
        });
    }

    public void delete(final Maintenance maintenance, final Callback<Boolean> callback) {
        executors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                database.maintenanceDao().delete(maintenance);
                if (callback != null) {
                    executors.mainThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(true);
                        }
                    });
                }
            }
        });
    }
}
