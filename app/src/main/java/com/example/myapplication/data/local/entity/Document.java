package com.example.myapplication.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Nota fiscal / documento vinculado a uma manutenção. */
@Entity(tableName = "documents",
        foreignKeys = @ForeignKey(entity = Maintenance.class,
                parentColumns = "id",
                childColumns = "maintenanceId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("maintenanceId")})
public class Document {

    public static final String TYPE_XML = "XML";
    public static final String TYPE_PDF = "PDF";
    public static final String TYPE_PHOTO = "PHOTO";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public Long maintenanceId;

    public String type;

    public String uri;

    public String invoiceNumber;

    public String cnpj;

    public String companyName;

    public long issueDate;

    public double totalValue;

    /** Texto bruto extraído (XML/OCR). Útil para reprocessar com IA depois. */
    public String rawText;

    public long createdAt;
}
