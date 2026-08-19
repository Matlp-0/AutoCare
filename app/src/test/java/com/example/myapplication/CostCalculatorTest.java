package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.data.local.entity.MaintenanceItem;
import com.example.myapplication.data.local.entity.OdometerReading;
import com.example.myapplication.data.local.entity.Refuel;
import com.example.myapplication.data.local.relation.MaintenanceWithItems;
import com.example.myapplication.domain.finance.CostCalculator;
import com.example.myapplication.domain.finance.CostSummary;
import com.example.myapplication.domain.model.MaintenanceType;
import com.example.myapplication.util.DateUtils;

import org.junit.Test;

import java.util.Calendar;

public class CostCalculatorTest {

    private final CostCalculator calculator = new CostCalculator();
    private final long now = now();

    private long now() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2026, Calendar.JUNE, 15, 12, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long daysAgo(int days) {
        return now - (days * DateUtils.DAY_MILLIS);
    }

    private MaintenanceWithItems maintenance(int daysAgo, int km, double cost, String category) {
        MaintenanceWithItems entry = new MaintenanceWithItems();
        entry.maintenance = new Maintenance();
        entry.maintenance.id = km;
        entry.maintenance.date = daysAgo(daysAgo);
        entry.maintenance.odometerKm = km;
        entry.maintenance.totalCost = cost;
        entry.maintenance.category = category;
        return entry;
    }

    private MaintenanceItem item(String type, double total) {
        MaintenanceItem item = new MaintenanceItem();
        item.type = type;
        item.totalPrice = total;
        return item;
    }

    private Refuel refuel(int daysAgo, int km, double liters, double price) {
        Refuel refuel = new Refuel();
        refuel.date = daysAgo(daysAgo);
        refuel.odometerKm = km;
        refuel.liters = liters;
        refuel.pricePerLiter = price;
        refuel.totalCost = liters * price;
        refuel.fullTank = true;
        return refuel;
    }

    @Test
    public void semRegistros_naoInventaNumero() {
        CostSummary summary = calculator.calculate(new CostCalculator.Input());

        assertEquals(0d, summary.totalSpent(), 0.0001d);
        assertFalse(summary.hasCostPerKm());
        assertEquals(CostCalculator.MONTHS_WINDOW, summary.months.size());
    }

    @Test
    public void custoPorKmUsaDistanciaCobertaPelosRegistros() {
        CostCalculator.Input input = new CostCalculator.Input();
        input.now = now;
        input.maintenances.add(maintenance(200, 50_000, 600d, MaintenanceType.OIL_CHANGE.name()));
        input.refuels.add(refuel(150, 51_000, 40d, 5d));
        input.refuels.add(refuel(30, 55_000, 40d, 5d));

        CostSummary summary = calculator.calculate(input);

        // 5.000 km cobertos, R$ 600 de manutenção + R$ 400 de combustível.
        assertEquals(5_000, summary.coveredKm);
        assertEquals(1_000d, summary.totalSpent(), 0.0001d);
        assertEquals(0.2d, summary.costPerKm, 0.0001d);
        assertEquals(0.12d, summary.maintenanceCostPerKm, 0.0001d);
        assertEquals(0.08d, summary.fuelCostPerKm, 0.0001d);
        assertEquals(0.4d, summary.fuelShare(), 0.0001d);
    }

    @Test
    public void leituraDeHodometroAmpliaAJanelaDeDistancia() {
        CostCalculator.Input input = new CostCalculator.Input();
        input.now = now;
        input.maintenances.add(maintenance(100, 80_000, 300d, MaintenanceType.BRAKE_PADS.name()));
        input.readings.add(OdometerReading.of(1L, daysAgo(200), 70_000,
                OdometerReading.SOURCE_MANUAL));
        input.readings.add(OdometerReading.of(1L, daysAgo(10), 90_000,
                OdometerReading.SOURCE_MANUAL));

        CostSummary summary = calculator.calculate(input);

        assertEquals(20_000, summary.coveredKm);
        assertEquals(0.015d, summary.costPerKm, 0.0001d);
    }

    @Test
    public void semItensPrecificados_tudoVaiParaACategoriaDaManutencao() {
        CostCalculator.Input input = new CostCalculator.Input();
        input.now = now;
        input.maintenances.add(maintenance(20, 10_000, 500d, MaintenanceType.SUSPENSION.name()));

        CostSummary summary = calculator.calculate(input);

        assertEquals(1, summary.categories.size());
        assertEquals(MaintenanceType.SUSPENSION, summary.categories.get(0).type);
        assertEquals(500d, summary.categories.get(0).amount, 0.0001d);
        assertEquals(1d, summary.categories.get(0).share, 0.0001d);
    }

    @Test
    public void comItens_rateiaOTotalDaNotaEasSomaBate() {
        CostCalculator.Input input = new CostCalculator.Input();
        input.now = now;
        MaintenanceWithItems entry = maintenance(20, 10_000, 600d,
                MaintenanceType.OIL_CHANGE.name());
        entry.items.add(item(MaintenanceType.OIL_CHANGE.name(), 300d));
        entry.items.add(item(MaintenanceType.OIL_FILTER.name(), 100d));
        input.maintenances.add(entry);

        CostSummary summary = calculator.calculate(input);

        // Itens somam 400, nota é 600 (mão de obra): rateio mantém a soma em 600.
        double total = 0d;
        for (CostSummary.CategoryCost category : summary.categories) {
            total += category.amount;
        }
        assertEquals(600d, total, 0.0001d);
        assertEquals(MaintenanceType.OIL_CHANGE, summary.categories.get(0).type);
        assertEquals(450d, summary.categories.get(0).amount, 0.0001d);
        assertEquals(0.75d, summary.categories.get(0).share, 0.0001d);
    }

    @Test
    public void notaSemTotalDeclarado_usaASomaDasPecas() {
        CostCalculator.Input input = new CostCalculator.Input();
        input.now = now;
        MaintenanceWithItems entry = maintenance(10, 10_000, 0d, MaintenanceType.OTHER.name());
        entry.items.add(item(MaintenanceType.SPARK_PLUGS.name(), 180d));
        entry.items.add(item(MaintenanceType.AIR_FILTER.name(), 120d));
        input.maintenances.add(entry);

        CostSummary summary = calculator.calculate(input);

        assertEquals(300d, summary.maintenanceSpent, 0.0001d);
        assertEquals(300d, summary.currentMonthSpent, 0.0001d);
        assertEquals(2, summary.categories.size());
        assertEquals(0.6d, summary.categories.get(0).share, 0.0001d);
    }

    @Test
    public void serieMensalTemDozeMesesEIgnoraOqueEstaForaDaJanela() {
        CostCalculator.Input input = new CostCalculator.Input();
        input.now = now;
        input.maintenances.add(maintenance(5, 10_000, 200d, MaintenanceType.OIL_CHANGE.name()));
        input.maintenances.add(maintenance(800, 5_000, 900d, MaintenanceType.TIRES.name()));

        CostSummary summary = calculator.calculate(input);

        assertEquals(12, summary.months.size());
        assertEquals(200d, summary.currentMonthSpent, 0.0001d);
        // A manutenção antiga entra no total, mas não no gráfico dos 12 meses.
        assertEquals(1_100d, summary.totalSpent(), 0.0001d);
        assertEquals(200d, summary.maxMonthTotal(), 0.0001d);
    }

    @Test
    public void projecaoAnualUsaORitmoDeUso() {
        CostCalculator.Input input = new CostCalculator.Input();
        input.now = now;
        input.kmPerDay = 30f;
        input.maintenances.add(maintenance(200, 10_000, 500d, MaintenanceType.OIL_CHANGE.name()));
        input.refuels.add(refuel(10, 15_000, 100d, 5d));

        CostSummary summary = calculator.calculate(input);

        // 5.000 km cobertos, R$ 1.000 gastos = R$ 0,20/km; 10.950 km por ano.
        assertEquals(10_950, summary.projectedYearlyKm);
        assertEquals(2_190d, summary.projectedYearlyCost, 0.01d);
        assertTrue(summary.hasCostPerKm());
    }
}
