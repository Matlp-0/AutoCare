package com.example.myapplication.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.data.local.relation.MaintenanceWithItems;

import java.util.List;

@Dao
public interface MaintenanceDao {

    @Insert
    long insert(Maintenance maintenance);

    @Update
    void update(Maintenance maintenance);

    @Delete
    void delete(Maintenance maintenance);

    @Transaction
    @Query("SELECT * FROM maintenances WHERE vehicleId = :vehicleId ORDER BY date DESC, id DESC")
    LiveData<List<MaintenanceWithItems>> observeByVehicle(long vehicleId);

    @Transaction
    @Query("SELECT * FROM maintenances WHERE id = :id")
    LiveData<MaintenanceWithItems> observeById(long id);

    @Query("DELETE FROM maintenances WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM maintenances WHERE id = :id")
    Maintenance findById(long id);

    @Query("SELECT * FROM maintenances WHERE vehicleId = :vehicleId ORDER BY date DESC, id DESC")
    List<Maintenance> findByVehicle(long vehicleId);

    @Query("SELECT COUNT(*) FROM maintenances WHERE vehicleId = :vehicleId")
    int countByVehicle(long vehicleId);
}
