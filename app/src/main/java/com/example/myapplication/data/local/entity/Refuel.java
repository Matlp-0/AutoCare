package com.example.myapplication.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Abastecimento registrado pelo usuário. */
@Entity(tableName = "refuels",
        foreignKeys = @ForeignKey(entity = Vehicle.class,
                parentColumns = "id",
                childColumns = "vehicleId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("vehicleId")})
public class Refuel {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long vehicleId;

    /** Data do abastecimento em millis. */
    public long date;

    public int odometerKm;

    public double liters;

    public double pricePerLiter;

    public double totalCost;

    /** Nome do enum FuelType. */
    public String fuelType;

    public String station;

    /**
     * Tanque cheio. O consumo só é calculável entre dois abastecimentos completos:
     * é o que garante que os litros gastos correspondem à distância percorrida.
     */
    public boolean fullTank = true;

    /**
     * Marca que algum abastecimento anterior não foi registrado. O trecho até o
     * abastecimento anterior é descartado no cálculo de consumo.
     */
    public boolean missedPrevious;

    public String notes;

    public long createdAt;
}
