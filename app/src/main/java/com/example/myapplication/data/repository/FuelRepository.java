package com.example.myapplication.data.repository;

import androidx.lifecycle.LiveData;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.entity.OdometerReading;
import com.example.myapplication.data.local.entity.Refuel;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.Callback;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Abastecimentos. Todo registro também vira leitura de hodômetro: é de graça
 * (o usuário já digitou a km) e alimenta a estimativa de km/dia.
 */
public class FuelRepository {

    private final AppDatabase database;
    private final AppExecutors executors = AppExecutors.get();

    public FuelRepository(AppDatabase database) {
        this.database = database;
    }

    public LiveData<List<Refuel>> observeByVehicle(long vehicleId) {
        return database.refuelDao().observeByVehicle(vehicleId);
    }

    public LiveData<Refuel> observeById(long id) {
        return database.refuelDao().observeById(id);
    }

    public List<Refuel> findByVehicleSync(long vehicleId) {
        return database.refuelDao().findByVehicle(vehicleId);
    }

    public void save(final Refuel refuel, final Callback<Long> callback) {
        executors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final long id = database.runInTransaction(new Callable<Long>() {
                    @Override
                    public Long call() {
                        boolean isNew = refuel.id <= 0;
                        if (isNew) {
                            refuel.createdAt = System.currentTimeMillis();
                        }
                        long refuelId;
                        if (isNew) {
                            refuelId = database.refuelDao().insert(refuel);
                        } else {
                            Refuel previous = database.refuelDao().findById(refuel.id);
                            if (previous != null) {
                                database.odometerReadingDao().deleteMatching(previous.vehicleId,
                                        OdometerReading.SOURCE_REFUEL, previous.date,
                                        previous.odometerKm);
                            }
                            database.refuelDao().update(refuel);
                            refuelId = refuel.id;
                        }
                        if (refuel.odometerKm > 0) {
                            database.odometerReadingDao().insert(OdometerReading.of(
                                    refuel.vehicleId, refuel.date, refuel.odometerKm,
                                    OdometerReading.SOURCE_REFUEL));
                            Vehicle vehicle = database.vehicleDao().findById(refuel.vehicleId);
                            if (vehicle != null && refuel.odometerKm > vehicle.currentKm) {
                                database.vehicleDao().updateKm(vehicle.id, refuel.odometerKm,
                                        System.currentTimeMillis());
                            }
                        }
                        return refuelId;
                    }
                });
                notifyResult(callback, id);
            }
        });
    }

    public void deleteById(final long refuelId, final Callback<Boolean> callback) {
        executors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                database.runInTransaction(new Runnable() {
                    @Override
                    public void run() {
                        Refuel refuel = database.refuelDao().findById(refuelId);
                        if (refuel == null) {
                            return;
                        }
                        database.odometerReadingDao().deleteMatching(refuel.vehicleId,
                                OdometerReading.SOURCE_REFUEL, refuel.date, refuel.odometerKm);
                        database.refuelDao().deleteById(refuelId);
                    }
                });
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

    private void notifyResult(final Callback<Long> callback, final long value) {
        if (callback == null) {
            return;
        }
        executors.mainThread(new Runnable() {
            @Override
            public void run() {
                callback.onResult(value);
            }
        });
    }
}
