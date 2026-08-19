package com.example.myapplication.data.repository;

import androidx.lifecycle.LiveData;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.entity.OdometerReading;
import com.example.myapplication.util.AppExecutors;

import java.util.List;

/** Série histórica do hodômetro, base do cálculo de km/dia. */
public class OdometerRepository {

    private final AppDatabase database;
    private final AppExecutors executors = AppExecutors.get();

    public OdometerRepository(AppDatabase database) {
        this.database = database;
    }

    public LiveData<List<OdometerReading>> observeByVehicle(long vehicleId) {
        return database.odometerReadingDao().observeByVehicle(vehicleId);
    }

    public List<OdometerReading> findByVehicleSync(long vehicleId) {
        return database.odometerReadingDao().findByVehicle(vehicleId);
    }

    public OdometerReading findLastSync(long vehicleId) {
        return database.odometerReadingDao().findLast(vehicleId);
    }

    /** Grava uma leitura avulsa (usada por origens que não passam por outro repo). */
    public void record(final long vehicleId, final int km, final long date, final String source) {
        executors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                database.odometerReadingDao().insert(
                        OdometerReading.of(vehicleId, date, km, source));
            }
        });
    }
}
