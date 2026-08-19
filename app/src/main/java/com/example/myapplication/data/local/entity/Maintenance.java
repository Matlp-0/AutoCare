package com.example.myapplication.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "maintenances",
        foreignKeys = @ForeignKey(entity = Vehicle.class,
                parentColumns = "id",
                childColumns = "vehicleId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("vehicleId")})
public class Maintenance {

    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_XML = "XML";
    public static final String SOURCE_PDF = "PDF";
    public static final String SOURCE_PHOTO = "PHOTO";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long vehicleId;

    /** Data da manutenção em millis. */
    public long date;

    public int odometerKm;

    /** Nome do enum MaintenanceType. */
    public String category;

    public String description;

    public double totalCost;

    public String workshop;

    public String notes;

    public String source = SOURCE_MANUAL;

    public long createdAt;

    public boolean synced;
}
