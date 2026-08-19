package com.example.myapplication.domain.manual;

import com.example.myapplication.domain.model.MaintenanceType;

/**
 * Item de uma revisão como aparece no plano do fabricante.
 * Guardamos o nome original ("Filtro de Ar-Condicionado") e o tipo interno usado
 * pelo motor de manutenção — nem todo item do manual tem tipo equivalente.
 */
public class ManualPlanItem {

    public final String label;
    public final MaintenanceType type;

    public ManualPlanItem(String label, MaintenanceType type) {
        this.label = label;
        this.type = type == null ? MaintenanceType.OTHER : type;
    }

    public ManualPlanItem(MaintenanceType type) {
        this(type.label(), type);
    }
}
