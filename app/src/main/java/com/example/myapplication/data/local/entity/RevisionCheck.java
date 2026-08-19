package com.example.myapplication.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Marcação do usuário para uma revisão do cronograma: "essa eu já fiz" / "essa não fiz".
 * Vale mais do que a dedução automática pelo histórico.
 */
@Entity(tableName = "revision_checks",
        foreignKeys = @ForeignKey(entity = Vehicle.class,
                parentColumns = "id",
                childColumns = "vehicleId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = {"vehicleId", "km"}, unique = true)})
public class RevisionCheck {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long vehicleId;

    /** Quilometragem da revisão (ex.: 40000). */
    public int km;

    public boolean done;

    /** Quando o usuário marcou como realizada. */
    public long doneDate;

    public long updatedAt;
}
