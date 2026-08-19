package com.example.myapplication.data.local.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.myapplication.data.local.entity.Document;
import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.data.local.entity.MaintenanceItem;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceWithItems {

    @Embedded
    public Maintenance maintenance;

    @Relation(parentColumn = "id", entityColumn = "maintenanceId")
    public List<MaintenanceItem> items = new ArrayList<>();

    @Relation(parentColumn = "id", entityColumn = "maintenanceId")
    public List<Document> documents = new ArrayList<>();
}
