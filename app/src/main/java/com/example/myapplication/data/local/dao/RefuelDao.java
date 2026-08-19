package com.example.myapplication.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.data.local.entity.Refuel;

import java.util.List;

@Dao
public interface RefuelDao {

    @Insert
    long insert(Refuel refuel);

    @Update
    void update(Refuel refuel);

    @Delete
    void delete(Refuel refuel);

    @Query("SELECT * FROM refuels WHERE vehicleId = :vehicleId ORDER BY date DESC, odometerKm DESC")
    LiveData<List<Refuel>> observeByVehicle(long vehicleId);

    @Query("SELECT * FROM refuels WHERE vehicleId = :vehicleId ORDER BY date ASC, odometerKm ASC")
    List<Refuel> findByVehicle(long vehicleId);

    @Query("SELECT * FROM refuels WHERE id = :id")
    Refuel findById(long id);

    @Query("SELECT * FROM refuels WHERE id = :id")
    LiveData<Refuel> observeById(long id);

    @Query("DELETE FROM refuels WHERE id = :id")
    void deleteById(long id);
}
