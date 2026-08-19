package com.example.myapplication.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Peça/serviço que compõe uma manutenção. */
@Entity(tableName = "maintenance_items",
        foreignKeys = @ForeignKey(entity = Maintenance.class,
                parentColumns = "id",
                childColumns = "maintenanceId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("maintenanceId")})
public class MaintenanceItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long maintenanceId;

    /** Nome do enum MaintenanceType identificado para o item. */
    public String type;

    public String name;

    public double quantity = 1d;

    public double unitPrice;

    public double totalPrice;
}
