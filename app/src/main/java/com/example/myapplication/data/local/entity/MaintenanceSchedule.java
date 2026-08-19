package com.example.myapplication.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Snapshot persistido do cronograma calculado pelo MaintenanceScheduler.
 * Permite mostrar dados imediatamente ao abrir o app (offline) antes do recálculo.
 */
@Entity(tableName = "maintenance_schedules",
        foreignKeys = @ForeignKey(entity = Vehicle.class,
                parentColumns = "id",
                childColumns = "vehicleId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("vehicleId")})
public class MaintenanceSchedule {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long vehicleId;

    public String type;

    public String label;

    public int intervalKm;

    public int intervalMonths;

    public int lastDoneKm = -1;

    public long lastDoneDate;

    public int nextDueKm;

    public long nextDueDate;

    /** Nome do enum MaintenanceStatus. */
    public String status;

    public long updatedAt;
}
