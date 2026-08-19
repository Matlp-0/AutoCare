package com.example.myapplication.domain.fuel;

/** Números derivados dos abastecimentos. Zero significa "ainda sem base". */
public class FuelStats {

    /** Consumo médio ponderado (km/l), entre tanques cheios. */
    public double averageConsumption;

    /** Consumo do último trecho completo (km/l). */
    public double lastConsumption;

    public double bestConsumption;

    public double worstConsumption;

    /** Custo por quilômetro rodado no período coberto pelos abastecimentos. */
    public double costPerKm;

    public double totalLiters;

    public double totalSpent;

    public double averagePricePerLiter;

    /** Gasto nos últimos 30 dias. */
    public double spentLast30Days;

    /** Média mensal do gasto no período coberto. */
    public double monthlyAverage;

    public int refuelCount;

    /** Trechos completos usados no cálculo de consumo. */
    public int measuredSegments;

    public long lastRefuelDate;

    public int lastRefuelKm;

    public double lastRefuelLiters;

    /** Distância coberta pelos abastecimentos (do primeiro ao último). */
    public int coveredKm;

    public boolean hasConsumption() {
        return averageConsumption > 0d;
    }

    /**
     * Autonomia restante estimada: o que o último tanque ainda deve render,
     * descontado o que já foi rodado desde ele. Zero quando não há base.
     */
    public int estimatedRangeKm(int currentKm) {
        if (averageConsumption <= 0d || lastRefuelLiters <= 0d || lastRefuelKm <= 0) {
            return 0;
        }
        double tankRange = lastRefuelLiters * averageConsumption;
        int driven = Math.max(0, currentKm - lastRefuelKm);
        return (int) Math.max(0d, tankRange - driven);
    }
}
