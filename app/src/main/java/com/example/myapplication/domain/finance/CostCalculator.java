package com.example.myapplication.domain.finance;

import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.data.local.entity.MaintenanceItem;
import com.example.myapplication.data.local.entity.OdometerReading;
import com.example.myapplication.data.local.entity.Refuel;
import com.example.myapplication.data.local.relation.MaintenanceWithItems;
import com.example.myapplication.domain.model.MaintenanceType;
import com.example.myapplication.util.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Painel financeiro: junta manutenção e combustível para responder quanto o carro
 * custa por quilômetro.
 *
 * <p>O custo por km é calculado sobre a distância que os próprios registros
 * cobrem (primeira e última leitura de hodômetro conhecida), não sobre a km total
 * do veículo: gasto lançado nos últimos dois anos dividido pela vida inteira do
 * carro daria um número bonito e falso. Sem dependência de Android — testável.
 */
public class CostCalculator {

    /** Quantos meses a série mensal cobre. */
    public static final int MONTHS_WINDOW = 12;

    public static class Input {
        public List<MaintenanceWithItems> maintenances = new ArrayList<>();
        public List<Refuel> refuels = new ArrayList<>();
        public List<OdometerReading> readings = new ArrayList<>();
        public long now = System.currentTimeMillis();
        /** Ritmo de uso, para projetar o custo do ano; 0 quando desconhecido. */
        public float kmPerDay;
    }

    public CostSummary calculate(Input input) {
        CostSummary summary = new CostSummary();
        if (input == null) {
            return summary;
        }

        Map<MaintenanceType, CostSummary.CategoryCost> byCategory =
                new EnumMap<>(MaintenanceType.class);
        Map<Integer, CostSummary.MonthCost> byMonth = monthSkeleton(input.now);

        for (MaintenanceWithItems entry : input.maintenances) {
            Maintenance maintenance = entry.maintenance;
            double cost = maintenanceCost(entry);
            summary.maintenanceSpent += cost;
            summary.maintenanceCount++;
            trackPeriod(summary, maintenance.date);

            CostSummary.MonthCost month = byMonth.get(monthKey(maintenance.date));
            if (month != null) {
                month.maintenance += cost;
            }
            splitByCategory(byCategory, entry, cost);
        }

        for (Refuel refuel : input.refuels) {
            double cost = refuelCost(refuel);
            summary.fuelSpent += cost;
            summary.refuelCount++;
            trackPeriod(summary, refuel.date);

            CostSummary.MonthCost month = byMonth.get(monthKey(refuel.date));
            if (month != null) {
                month.fuel += cost;
            }
        }

        summary.months.addAll(byMonth.values());
        Collections.sort(summary.months, new Comparator<CostSummary.MonthCost>() {
            @Override
            public int compare(CostSummary.MonthCost left, CostSummary.MonthCost right) {
                int byYear = Integer.compare(left.year, right.year);
                return byYear != 0 ? byYear : Integer.compare(left.month, right.month);
            }
        });

        summary.currentMonthSpent = currentMonthSpent(summary, input.now);
        summary.coveredKm = coveredKm(input);
        summary.coveredDays = summary.firstRecordDate > 0
                ? DateUtils.daysBetween(summary.firstRecordDate, summary.lastRecordDate) : 0;

        if (summary.coveredKm > 0) {
            summary.costPerKm = summary.totalSpent() / summary.coveredKm;
            summary.maintenanceCostPerKm = summary.maintenanceSpent / summary.coveredKm;
            summary.fuelCostPerKm = summary.fuelSpent / summary.coveredKm;
        }
        if (summary.coveredDays >= 30) {
            summary.monthlyAverage = summary.totalSpent() / (summary.coveredDays / 30d);
        } else if (summary.totalSpent() > 0d) {
            // Período curto: extrapolar mês cheio enganaria; mostra o que houve.
            summary.monthlyAverage = summary.totalSpent();
        }
        if (input.kmPerDay > 0f && summary.costPerKm > 0d) {
            summary.projectedYearlyKm = Math.round(input.kmPerDay * 365f);
            summary.projectedYearlyCost = summary.projectedYearlyKm * summary.costPerKm;
        }

        finishCategories(summary, byCategory);
        return summary;
    }

    /**
     * Distribui o custo da manutenção entre os itens quando eles têm preço; sem
     * itens precificados, tudo vai para a categoria da própria manutenção.
     */
    private void splitByCategory(Map<MaintenanceType, CostSummary.CategoryCost> byCategory,
                                 MaintenanceWithItems entry, double cost) {
        double itemsTotal = itemsTotal(entry);

        if (itemsTotal <= 0d) {
            add(byCategory, MaintenanceType.fromName(entry.maintenance.category), cost);
            return;
        }
        // Rateia o valor real da nota (que pode incluir mão de obra) na proporção
        // dos itens, para a soma das categorias bater com o total gasto.
        double factor = cost / itemsTotal;
        for (MaintenanceItem item : entry.items) {
            add(byCategory, MaintenanceType.fromName(item.type), itemCost(item) * factor);
        }
    }

    private void add(Map<MaintenanceType, CostSummary.CategoryCost> byCategory,
                     MaintenanceType type, double amount) {
        if (amount <= 0d) {
            return;
        }
        CostSummary.CategoryCost cost = byCategory.get(type);
        if (cost == null) {
            cost = new CostSummary.CategoryCost();
            cost.type = type;
            cost.label = type.label();
            byCategory.put(type, cost);
        }
        cost.amount += amount;
        cost.occurrences++;
    }

    private void finishCategories(CostSummary summary,
                                  Map<MaintenanceType, CostSummary.CategoryCost> byCategory) {
        for (CostSummary.CategoryCost cost : byCategory.values()) {
            cost.share = summary.maintenanceSpent > 0d ? cost.amount / summary.maintenanceSpent : 0d;
            summary.categories.add(cost);
        }
        Collections.sort(summary.categories, new Comparator<CostSummary.CategoryCost>() {
            @Override
            public int compare(CostSummary.CategoryCost left, CostSummary.CategoryCost right) {
                return Double.compare(right.amount, left.amount);
            }
        });
    }

    /**
     * Distância coberta pelos registros. As leituras de hodômetro são a fonte
     * principal; manutenções e abastecimentos entram como reforço.
     */
    private int coveredKm(Input input) {
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (OdometerReading reading : input.readings) {
            if (reading.km <= 0) {
                continue;
            }
            min = Math.min(min, reading.km);
            max = Math.max(max, reading.km);
        }
        for (MaintenanceWithItems entry : input.maintenances) {
            int km = entry.maintenance.odometerKm;
            if (km > 0) {
                min = Math.min(min, km);
                max = Math.max(max, km);
            }
        }
        for (Refuel refuel : input.refuels) {
            if (refuel.odometerKm > 0) {
                min = Math.min(min, refuel.odometerKm);
                max = Math.max(max, refuel.odometerKm);
            }
        }
        return min == Integer.MAX_VALUE ? 0 : Math.max(0, max - min);
    }

    private double currentMonthSpent(CostSummary summary, long now) {
        int key = monthKey(now);
        for (CostSummary.MonthCost month : summary.months) {
            if (month.year * 100 + month.month == key) {
                return month.total();
            }
        }
        return 0d;
    }

    /** Meses da janela já criados, para que meses sem gasto apareçam como zero. */
    private Map<Integer, CostSummary.MonthCost> monthSkeleton(long now) {
        Map<Integer, CostSummary.MonthCost> months = new java.util.LinkedHashMap<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        calendar.add(Calendar.MONTH, -(MONTHS_WINDOW - 1));
        for (int index = 0; index < MONTHS_WINDOW; index++) {
            CostSummary.MonthCost month = new CostSummary.MonthCost();
            month.year = calendar.get(Calendar.YEAR);
            month.month = calendar.get(Calendar.MONTH) + 1;
            months.put(month.year * 100 + month.month, month);
            calendar.add(Calendar.MONTH, 1);
        }
        return months;
    }

    private int monthKey(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH) + 1;
    }

    private void trackPeriod(CostSummary summary, long date) {
        if (date <= 0) {
            return;
        }
        if (summary.firstRecordDate == 0L || date < summary.firstRecordDate) {
            summary.firstRecordDate = date;
        }
        if (date > summary.lastRecordDate) {
            summary.lastRecordDate = date;
        }
    }

    /**
     * Custo da manutenção. Nota importada sem total declarado cai na soma das
     * peças: o gasto existiu, e ignorá-lo distorceria o custo por km para menos.
     */
    private double maintenanceCost(MaintenanceWithItems entry) {
        return entry.maintenance.totalCost > 0d ? entry.maintenance.totalCost : itemsTotal(entry);
    }

    private double itemsTotal(MaintenanceWithItems entry) {
        double total = 0d;
        for (MaintenanceItem item : entry.items) {
            total += itemCost(item);
        }
        return total;
    }

    private double itemCost(MaintenanceItem item) {
        return item.totalPrice > 0d ? item.totalPrice : item.unitPrice * item.quantity;
    }

    private double refuelCost(Refuel refuel) {
        return refuel.totalCost > 0d ? refuel.totalCost : refuel.liters * refuel.pricePerLiter;
    }
}
