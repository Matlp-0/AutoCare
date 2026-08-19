package com.example.myapplication.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Leitura do hodômetro em uma data. É o insumo do cálculo de km/dia, que
 * transforma prazos em quilometragem ("faltam 2.500 km") em prazos em data.
 *
 * <p>Toda origem que conhece a km do veículo grava aqui: atualização manual,
 * abastecimento e manutenção.
 */
@Entity(tableName = "odometer_readings",
        foreignKeys = @ForeignKey(entity = Vehicle.class,
                parentColumns = "id",
                childColumns = "vehicleId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("vehicleId")})
public class OdometerReading {

    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_REFUEL = "REFUEL";
    public static final String SOURCE_MAINTENANCE = "MAINTENANCE";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long vehicleId;

    /** Data da leitura em millis. */
    public long date;

    public int km;

    public String source = SOURCE_MANUAL;

    public long createdAt;

    public static OdometerReading of(long vehicleId, long date, int km, String source) {
        OdometerReading reading = new OdometerReading();
        reading.vehicleId = vehicleId;
        reading.date = date;
        reading.km = km;
        reading.source = source;
        reading.createdAt = System.currentTimeMillis();
        return reading;
    }
}
