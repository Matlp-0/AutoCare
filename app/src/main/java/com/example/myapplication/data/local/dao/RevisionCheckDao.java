package com.example.myapplication.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.data.local.entity.RevisionCheck;

import java.util.List;

@Dao
public interface RevisionCheckDao {

    /** O índice único (vehicleId, km) faz a marcação ser substituída, não duplicada. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(RevisionCheck check);

    @Query("SELECT * FROM revision_checks WHERE vehicleId = :vehicleId")
    List<RevisionCheck> findByVehicle(long vehicleId);

    @Query("DELETE FROM revision_checks WHERE vehicleId = :vehicleId AND km = :km")
    void delete(long vehicleId, int km);
}
