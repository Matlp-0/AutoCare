package com.example.myapplication.domain.finance;

import com.example.myapplication.domain.model.MaintenanceType;

import java.util.ArrayList;
import java.util.List;

/** Retrato financeiro do veículo. Zero significa "ainda sem base". */
public class CostSummary {

    /** Gasto de um mês do calendário. */
    public static class MonthCost {
        public int year;
        /** 1 = janeiro. */
        public int month;
        public double maintenance;
        public double fuel;

        public double total() {
            return maintenance + fuel;
        }
    }

    /** Quanto foi para cada tipo de item de manutenção. */
    public static class CategoryCost {
        public MaintenanceType type;
        public String label;
        public double amount;
        public int occurrences;
        /** Fatia do gasto de manutenção, de 0 a 1. */
        public double share;
    }

    public double maintenanceSpent;
    public double fuelSpent;

    /** Custo por km rodado no período coberto pelos registros. */
    public double costPerKm;
    public double maintenanceCostPerKm;
    public double fuelCostPerKm;

    /** Distância usada como base do custo por km. */
    public int coveredKm;
    public long firstRecordDate;
    public long lastRecordDate;
    public int coveredDays;

    public double currentMonthSpent;
    /** Média mensal no período coberto. */
    public double monthlyAverage;

    /** Projeção de 12 meses: custo por km × km que o veículo costuma rodar. */
    public double projectedYearlyCost;
    public int projectedYearlyKm;

    public int maintenanceCount;
    public int refuelCount;

    public final List<MonthCost> months = new ArrayList<>();
    public final List<CategoryCost> categories = new ArrayList<>();

    public double totalSpent() {
        return maintenanceSpent + fuelSpent;
    }

    public boolean hasCostPerKm() {
        return costPerKm > 0d;
    }

    /** Fatia do gasto que é combustível, de 0 a 1; 0 sem gasto nenhum. */
    public double fuelShare() {
        double total = totalSpent();
        return total > 0d ? fuelSpent / total : 0d;
    }

    public double maxMonthTotal() {
        double max = 0d;
        for (MonthCost month : months) {
            max = Math.max(max, month.total());
        }
        return max;
    }
}
