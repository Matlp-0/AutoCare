package com.example.myapplication.domain.model;

public enum MaintenanceStatus {

    /** Ainda não há plano nem histórico suficiente para afirmar qualquer coisa. */
    UNKNOWN("Sem dados suficientes"),
    OK("Manutenção em dia"),
    DUE_SOON("Atenção necessária"),
    OVERDUE("Manutenção atrasada");

    private final String label;

    MaintenanceStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static MaintenanceStatus fromName(String name) {
        if (name == null) {
            return UNKNOWN;
        }
        for (MaintenanceStatus status : values()) {
            if (status.name().equalsIgnoreCase(name)) {
                return status;
            }
        }
        return UNKNOWN;
    }

    /** Retorna o status mais crítico entre os dois. */
    public static MaintenanceStatus worst(MaintenanceStatus a, MaintenanceStatus b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
