package com.example.myapplication.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo de itens de manutenção com intervalos padrão (fallback quando o plano
 * do fabricante não cobre o item).
 */
public enum MaintenanceType {

    OIL_CHANGE("Troca de óleo", "🛢", 10000, 12),
    OIL_FILTER("Filtro de óleo", "🧴", 10000, 12),
    AIR_FILTER("Filtro de ar", "🔧", 20000, 24),
    CABIN_FILTER("Filtro de cabine", "🌬", 15000, 12),
    FUEL_FILTER("Filtro de combustível", "⛽", 40000, 36),
    SPARK_PLUGS("Velas de ignição", "⚡", 40000, 36),
    BRAKE_PADS("Pastilhas de freio", "🛑", 40000, 36),
    BRAKE_DISCS("Discos/tambores de freio", "🛑", 60000, 48),
    BRAKE_FLUID("Fluido de freio", "🧴", 40000, 24),
    COOLANT("Fluido de arrefecimento", "❄", 60000, 36),
    BELTS("Correias", "⚙", 60000, 60),
    BATTERY("Bateria", "🔋", 60000, 36),
    TIRES("Pneus", "🛞", 50000, 60),
    TIRE_ROTATION("Rodízio dos pneus", "🛞", 10000, 6),
    ALIGNMENT("Alinhamento e balanceamento", "📐", 10000, 12),
    SUSPENSION("Suspensão", "🔩", 60000, 48),
    TRANSMISSION_OIL("Óleo do câmbio", "⚙", 60000, 48),
    GENERAL_INSPECTION("Inspeção geral", "🔍", 10000, 12),
    OTHER("Outros", "🛠", 0, 0);

    private final String label;
    private final String icon;
    private final int defaultIntervalKm;
    private final int defaultIntervalMonths;

    MaintenanceType(String label, String icon, int defaultIntervalKm, int defaultIntervalMonths) {
        this.label = label;
        this.icon = icon;
        this.defaultIntervalKm = defaultIntervalKm;
        this.defaultIntervalMonths = defaultIntervalMonths;
    }

    public String label() {
        return label;
    }

    public String icon() {
        return icon;
    }

    public int defaultIntervalKm() {
        return defaultIntervalKm;
    }

    public int defaultIntervalMonths() {
        return defaultIntervalMonths;
    }

    public boolean isScheduled() {
        return defaultIntervalKm > 0 || defaultIntervalMonths > 0;
    }

    public static MaintenanceType fromName(String name) {
        if (name == null) {
            return OTHER;
        }
        for (MaintenanceType type : values()) {
            if (type.name().equalsIgnoreCase(name.trim())) {
                return type;
            }
        }
        return OTHER;
    }

    public static MaintenanceType fromLabel(String label) {
        if (label == null) {
            return OTHER;
        }
        for (MaintenanceType type : values()) {
            if (type.label.equalsIgnoreCase(label.trim())) {
                return type;
            }
        }
        return OTHER;
    }

    public static List<String> allLabels() {
        List<String> labels = new ArrayList<>();
        for (MaintenanceType type : values()) {
            labels.add(type.label);
        }
        return labels;
    }
}
