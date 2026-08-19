package com.example.myapplication.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.myapplication.data.local.entity.OdometerReading;

import java.util.List;

@Dao
public interface OdometerReadingDao {

    @Insert
    long insert(OdometerReading reading);

    @Query("SELECT * FROM odometer_readings WHERE vehicleId = :vehicleId ORDER BY date ASC, km ASC")
    List<OdometerReading> findByVehicle(long vehicleId);

    @Query("SELECT * FROM odometer_readings WHERE vehicleId = :vehicleId ORDER BY date ASC, km ASC")
    LiveData<List<OdometerReading>> observeByVehicle(long vehicleId);

    @Query("SELECT * FROM odometer_readings WHERE vehicleId = :vehicleId"
            + " ORDER BY date DESC, km DESC LIMIT 1")
    OdometerReading findLast(long vehicleId);

    @Query("DELETE FROM odometer_readings WHERE vehicleId = :vehicleId AND source = :source"
            + " AND date = :date AND km = :km")
    void deleteMatching(long vehicleId, String source, long date, int km);
}
