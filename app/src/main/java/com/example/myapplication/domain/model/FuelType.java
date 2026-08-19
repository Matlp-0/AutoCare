package com.example.myapplication.domain.model;

import java.util.ArrayList;
import java.util.List;

/** Combustíveis aceitos no registro de abastecimento. */
public enum FuelType {

    GASOLINE("Gasolina comum"),
    GASOLINE_ADDITIVE("Gasolina aditivada"),
    ETHANOL("Etanol"),
    DIESEL("Diesel"),
    DIESEL_S10("Diesel S-10"),
    GNV("GNV"),
    OTHER("Outro");

    private final String label;

    FuelType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static FuelType fromName(String name) {
        if (name == null) {
            return GASOLINE;
        }
        for (FuelType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return GASOLINE;
    }

    public static FuelType fromLabel(String label) {
        if (label != null) {
            for (FuelType type : values()) {
                if (type.label.equalsIgnoreCase(label.trim())) {
                    return type;
                }
            }
        }
        return GASOLINE;
    }

    public static List<String> allLabels() {
        List<String> labels = new ArrayList<>();
        for (FuelType type : values()) {
            labels.add(type.label);
        }
        return labels;
    }
}
