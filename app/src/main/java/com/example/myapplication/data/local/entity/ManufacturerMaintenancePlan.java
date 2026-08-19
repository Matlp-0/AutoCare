package com.example.myapplication.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Uma linha do plano de manutenção do fabricante (ex.: revisão de 10.000 km).
 * Populado pela consulta online do manual, com o plano padrão genérico como fallback.
 */
@Entity(tableName = "manufacturer_plans",
        indices = {@Index({"brand", "model", "engine"})})
public class ManufacturerMaintenancePlan {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String brand;

    public String model;

    public String engine;

    public int yearFrom;

    public int yearTo;

    /** Intervalo da revisão em km (ex.: 10000). */
    public int intervalKm;

    /** Intervalo equivalente em meses (ex.: 12). */
    public int intervalMonths;

    /** Nomes do enum MaintenanceType separados por vírgula. */
    public String items;

    /** Nomes originais dos itens no manual, separados por "|", na ordem de {@link #items}. */
    public String itemLabels;

    public String description;

    /** Origem dos dados: DEFAULT, WEB, AI, MANUAL. */
    public String source = "DEFAULT";

    /** Página de onde o plano foi extraído (quando veio da internet). */
    public String sourceUrl;

    public long updatedAt;
}
