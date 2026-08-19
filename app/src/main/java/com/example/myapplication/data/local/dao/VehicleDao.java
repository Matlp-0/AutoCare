package com.example.myapplication.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.data.local.entity.Vehicle;

import java.util.List;

@Dao
public interface VehicleDao {

    @Insert
    long insert(Vehicle vehicle);

    @Update
    void update(Vehicle vehicle);

    @Delete
    void delete(Vehicle vehicle);

    @Query("SELECT * FROM vehicles ORDER BY sortOrder, id LIMIT 1")
    LiveData<Vehicle> observeFirst();

    @Query("SELECT * FROM vehicles ORDER BY sortOrder, id LIMIT 1")
    Vehicle findFirst();

    @Query("SELECT * FROM vehicles WHERE id = :id")
    Vehicle findById(long id);

    @Query("SELECT * FROM vehicles ORDER BY sortOrder, id")
    LiveData<List<Vehicle>> observeAll();

    @Query("DELETE FROM vehicles WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM vehicles ORDER BY sortOrder, id")
    List<Vehicle> findAll();

    @Query("UPDATE vehicles SET sortOrder = :sortOrder WHERE id = :vehicleId")
    void updateSortOrder(long vehicleId, int sortOrder);

    @Query("SELECT MAX(sortOrder) FROM vehicles")
    Integer maxSortOrder();

    @Query("SELECT COUNT(*) FROM vehicles")
    int count();

    @Query("UPDATE vehicles SET currentKm = :km, updatedAt = :updatedAt WHERE id = :vehicleId")
    void updateKm(long vehicleId, int km, long updatedAt);
}
