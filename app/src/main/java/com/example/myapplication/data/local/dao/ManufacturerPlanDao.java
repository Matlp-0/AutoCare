package com.example.myapplication.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.myapplication.data.local.entity.ManufacturerMaintenancePlan;

import java.util.List;

@Dao
public interface ManufacturerPlanDao {

    @Insert
    void insertAll(List<ManufacturerMaintenancePlan> plans);

    @Query("SELECT * FROM manufacturer_plans WHERE LOWER(brand) = LOWER(:brand)"
            + " AND LOWER(model) = LOWER(:model) AND :year BETWEEN yearFrom AND yearTo"
            + " ORDER BY intervalKm")
    List<ManufacturerMaintenancePlan> findFor(String brand, String model, int year);

    @Query("SELECT COUNT(*) FROM manufacturer_plans")
    int count();

    /** Plano salvo para a marca/modelo, independente do ano (fallback). */
    @Query("SELECT * FROM manufacturer_plans WHERE LOWER(brand) = LOWER(:brand)"
            + " AND LOWER(model) = LOWER(:model) ORDER BY intervalKm")
    List<ManufacturerMaintenancePlan> findForBrandModel(String brand, String model);

    @Query("DELETE FROM manufacturer_plans WHERE LOWER(brand) = LOWER(:brand)"
            + " AND LOWER(model) = LOWER(:model)")
    void deleteForBrandModel(String brand, String model);
}
