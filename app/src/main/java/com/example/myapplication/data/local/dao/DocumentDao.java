package com.example.myapplication.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.myapplication.data.local.entity.Document;

import java.util.List;

@Dao
public interface DocumentDao {

    @Insert
    long insert(Document document);

    @Query("SELECT * FROM documents WHERE maintenanceId = :maintenanceId")
    List<Document> findByMaintenance(long maintenanceId);
}
