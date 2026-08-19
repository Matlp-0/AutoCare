package com.example.myapplication.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.myapplication.data.local.entity.MaintenanceSchedule;

import java.util.List;

@Dao
public interface MaintenanceScheduleDao {

    @Insert
    void insertAll(List<MaintenanceSchedule> schedules);

    @Query("SELECT * FROM maintenance_schedules WHERE vehicleId = :vehicleId ORDER BY nextDueKm")
    LiveData<List<MaintenanceSchedule>> observeByVehicle(long vehicleId);

    @Query("SELECT * FROM maintenance_schedules WHERE vehicleId = :vehicleId ORDER BY nextDueKm")
    List<MaintenanceSchedule> findByVehicle(long vehicleId);

    @Query("DELETE FROM maintenance_schedules WHERE vehicleId = :vehicleId")
    void deleteByVehicle(long vehicleId);
}
