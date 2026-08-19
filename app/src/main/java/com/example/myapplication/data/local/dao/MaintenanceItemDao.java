package com.example.myapplication.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.myapplication.data.local.entity.MaintenanceItem;

import java.util.List;

@Dao
public interface MaintenanceItemDao {

    @Insert
    long insert(MaintenanceItem item);

    @Insert
    void insertAll(List<MaintenanceItem> items);

    @Query("SELECT * FROM maintenance_items WHERE maintenanceId = :maintenanceId")
    List<MaintenanceItem> findByMaintenance(long maintenanceId);

    @Query("DELETE FROM maintenance_items WHERE maintenanceId = :maintenanceId")
    void deleteByMaintenance(long maintenanceId);

    @Query("SELECT i.* FROM maintenance_items i INNER JOIN maintenances m ON m.id = i.maintenanceId"
            + " WHERE m.vehicleId = :vehicleId")
    List<MaintenanceItem> findByVehicle(long vehicleId);
}
