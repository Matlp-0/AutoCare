package com.example.myapplication.domain.manual;

import com.example.myapplication.domain.model.MaintenanceType;

import java.util.ArrayList;
import java.util.List;

/** Uma revisão prevista pelo fabricante. */
public class ManualPlanEntry {

    /** Separador usado para persistir os nomes originais dos itens. */
    public static final String LABEL_SEPARATOR = "|";

    public final int intervalKm;
    public final int intervalMonths;
    public final String description;
    /** Itens como aparecem no plano (nome original + tipo interno). */
    public final List<ManualPlanItem> planItems;
    /** Tipos conhecidos pelo motor de manutenção, derivados de {@link #planItems}. */
    public final List<MaintenanceType> items;

    public ManualPlanEntry(int intervalKm, int intervalMonths, String description,
                           List<ManualPlanItem> planItems) {
        this.intervalKm = intervalKm;
        this.intervalMonths = intervalMonths;
        this.description = description;
        this.planItems = planItems == null ? new ArrayList<ManualPlanItem>() : planItems;
        this.items = new ArrayList<>();
        for (ManualPlanItem item : this.planItems) {
            if (item.type != MaintenanceType.OTHER && !this.items.contains(item.type)) {
                this.items.add(item.type);
            }
        }
    }

    public ManualPlanEntry(int intervalKm, int intervalMonths, String description,
                           MaintenanceType... types) {
        this(intervalKm, intervalMonths, description, toPlanItems(types));
    }

    private static List<ManualPlanItem> toPlanItems(MaintenanceType[] types) {
        List<ManualPlanItem> planItems = new ArrayList<>();
        if (types != null) {
            for (MaintenanceType type : types) {
                planItems.add(new ManualPlanItem(type));
            }
        }
        return planItems;
    }

    /** Nomes dos enums, para persistir na tabela de planos. */
    public String itemsCsv() {
        StringBuilder builder = new StringBuilder();
        for (ManualPlanItem item : planItems) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(item.type.name());
        }
        return builder.toString();
    }

    /** Nomes originais do manual, na mesma ordem de {@link #itemsCsv()}. */
    public String labelsCsv() {
        StringBuilder builder = new StringBuilder();
        for (ManualPlanItem item : planItems) {
            if (builder.length() > 0) {
                builder.append(LABEL_SEPARATOR);
            }
            builder.append(item.label);
        }
        return builder.toString();
    }

    /** Nomes para exibição, sem repetir o mesmo rótulo mapeado em vários tipos. */
    public List<String> labels() {
        List<String> labels = new ArrayList<>();
        for (ManualPlanItem item : planItems) {
            if (!labels.contains(item.label)) {
                labels.add(item.label);
            }
        }
        return labels;
    }
}
