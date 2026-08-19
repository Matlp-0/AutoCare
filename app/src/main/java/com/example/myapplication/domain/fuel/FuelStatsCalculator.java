package com.example.myapplication.domain.fuel;

import com.example.myapplication.data.local.entity.Refuel;
import com.example.myapplication.util.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consumo e custo a partir dos abastecimentos.
 *
 * <p>O consumo só é medido entre dois abastecimentos de tanque cheio: é a única
 * situação em que os litros gastos correspondem exatamente à distância rodada.
 * Trechos marcados como "não registrei o anterior" são descartados. Sem
 * dependência de Android — testável.
 */
public class FuelStatsCalculator {

    public FuelStats calculate(List<Refuel> refuels, long now) {
        FuelStats stats = new FuelStats();
        if (refuels == null || refuels.isEmpty()) {
            return stats;
        }

        List<Refuel> sorted = sortedCopy(refuels);

        stats.refuelCount = sorted.size();
        Refuel last = sorted.get(sorted.size() - 1);
        stats.lastRefuelDate = last.date;
        stats.lastRefuelKm = last.odometerKm;
        stats.lastRefuelLiters = last.fullTank ? last.liters : 0d;

        long thirtyDaysAgo = now - (30L * DateUtils.DAY_MILLIS);
        for (Refuel refuel : sorted) {
            stats.totalLiters += refuel.liters;
            stats.totalSpent += cost(refuel);
            if (refuel.date >= thirtyDaysAgo) {
                stats.spentLast30Days += cost(refuel);
            }
        }
        if (stats.totalLiters > 0d) {
            stats.averagePricePerLiter = stats.totalSpent / stats.totalLiters;
        }

        long spanDays = Math.max(1L,
                (last.date - sorted.get(0).date) / DateUtils.DAY_MILLIS);
        stats.monthlyAverage = stats.totalSpent / (spanDays / 30d);
        if (spanDays < 30L) {
            // Período curto: a média mensal extrapolada enganaria; usa o gasto real.
            stats.monthlyAverage = stats.totalSpent;
        }

        int firstKm = sorted.get(0).odometerKm;
        stats.coveredKm = Math.max(0, last.odometerKm - firstKm);

        measureConsumption(sorted, stats);
        return stats;
    }

    /** Consumo de cada trecho completo, indexado pelo abastecimento que o fecha. */
    public Map<Long, Double> consumptionByRefuel(List<Refuel> refuels) {
        Map<Long, Double> byRefuel = new HashMap<>();
        if (refuels == null || refuels.isEmpty()) {
            return byRefuel;
        }
        List<Refuel> sorted = sortedCopy(refuels);
        for (Segment segment : segments(sorted)) {
            byRefuel.put(segment.endId, segment.distance / segment.liters);
        }
        return byRefuel;
    }

    private void measureConsumption(List<Refuel> sorted, FuelStats stats) {
        double totalDistance = 0d;
        double totalLiters = 0d;
        double totalCost = 0d;

        for (Segment segment : segments(sorted)) {
            double consumption = segment.distance / segment.liters;
            stats.lastConsumption = consumption;
            if (stats.bestConsumption == 0d || consumption > stats.bestConsumption) {
                stats.bestConsumption = consumption;
            }
            if (stats.worstConsumption == 0d || consumption < stats.worstConsumption) {
                stats.worstConsumption = consumption;
            }
            totalDistance += segment.distance;
            totalLiters += segment.liters;
            totalCost += segment.cost;
            stats.measuredSegments++;
        }

        if (totalLiters > 0d && totalDistance > 0d) {
            stats.averageConsumption = totalDistance / totalLiters;
            stats.costPerKm = totalCost / totalDistance;
        }
    }

    /** Trecho válido entre dois tanques cheios. */
    private static class Segment {
        long endId;
        double distance;
        double liters;
        double cost;
    }

    /**
     * Percorre os abastecimentos acumulando litros até o próximo tanque cheio.
     * Trechos com abastecimento não registrado ou distância inválida são
     * descartados: melhor não ter número do que ter número errado.
     */
    private List<Segment> segments(List<Refuel> sorted) {
        List<Segment> segments = new ArrayList<>();
        Refuel openFull = null;
        double liters = 0d;
        double cost = 0d;
        boolean broken = false;

        for (Refuel refuel : sorted) {
            if (openFull == null) {
                if (refuel.fullTank) {
                    openFull = refuel;
                }
                continue;
            }

            liters += refuel.liters;
            cost += cost(refuel);
            if (refuel.missedPrevious) {
                broken = true;
            }
            if (!refuel.fullTank) {
                continue;
            }

            int distance = refuel.odometerKm - openFull.odometerKm;
            if (!broken && distance > 0 && liters > 0d) {
                Segment segment = new Segment();
                segment.endId = refuel.id;
                segment.distance = distance;
                segment.liters = liters;
                segment.cost = cost;
                segments.add(segment);
            }

            openFull = refuel;
            liters = 0d;
            cost = 0d;
            broken = false;
        }
        return segments;
    }

    private List<Refuel> sortedCopy(List<Refuel> refuels) {
        List<Refuel> sorted = new ArrayList<>(refuels);
        Collections.sort(sorted, new Comparator<Refuel>() {
            @Override
            public int compare(Refuel left, Refuel right) {
                int byDate = Long.compare(left.date, right.date);
                return byDate != 0 ? byDate : Integer.compare(left.odometerKm, right.odometerKm);
            }
        });
        return sorted;
    }

    private double cost(Refuel refuel) {
        if (refuel.totalCost > 0d) {
            return refuel.totalCost;
        }
        return refuel.liters * refuel.pricePerLiter;
    }
}
