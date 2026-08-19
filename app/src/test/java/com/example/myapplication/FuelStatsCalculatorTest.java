package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.myapplication.data.local.entity.Refuel;
import com.example.myapplication.domain.fuel.FuelStats;
import com.example.myapplication.domain.fuel.FuelStatsCalculator;
import com.example.myapplication.util.DateUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class FuelStatsCalculatorTest {

    private static final long NOW = 1_700_000_000_000L;

    private final FuelStatsCalculator calculator = new FuelStatsCalculator();

    private Refuel refuel(long daysAgo, int km, double liters, double price, boolean fullTank) {
        Refuel refuel = new Refuel();
        refuel.id = km;
        refuel.vehicleId = 1L;
        refuel.date = NOW - (daysAgo * DateUtils.DAY_MILLIS);
        refuel.odometerKm = km;
        refuel.liters = liters;
        refuel.pricePerLiter = price;
        refuel.totalCost = liters * price;
        refuel.fullTank = fullTank;
        return refuel;
    }

    @Test
    public void semAbastecimento_naoInventaNumero() {
        FuelStats stats = calculator.calculate(new ArrayList<Refuel>(), NOW);

        assertEquals(0d, stats.averageConsumption, 0.0001d);
        assertEquals(0, stats.refuelCount);
    }

    @Test
    public void umTanqueCheioSozinho_naoGeraConsumo() {
        List<Refuel> refuels = new ArrayList<>();
        refuels.add(refuel(10, 50_000, 40d, 5d, true));

        FuelStats stats = calculator.calculate(refuels, NOW);

        assertEquals(1, stats.refuelCount);
        assertEquals(0, stats.measuredSegments);
        assertEquals(0d, stats.averageConsumption, 0.0001d);
    }

    @Test
    public void consumoUsaDistanciaEntreTanquesCheios() {
        List<Refuel> refuels = new ArrayList<>();
        refuels.add(refuel(60, 50_000, 40d, 5d, true));
        refuels.add(refuel(30, 50_400, 40d, 5d, true));

        FuelStats stats = calculator.calculate(refuels, NOW);

        // 400 km com os 40 litros do segundo tanque = 10 km/l.
        assertEquals(10d, stats.averageConsumption, 0.0001d);
        assertEquals(1, stats.measuredSegments);
        assertEquals(0.5d, stats.costPerKm, 0.0001d);
    }

    @Test
    public void abastecimentoParcialEntraNaContaDoTrecho() {
        List<Refuel> refuels = new ArrayList<>();
        refuels.add(refuel(90, 10_000, 30d, 5d, true));
        refuels.add(refuel(60, 10_200, 20d, 5d, false));
        refuels.add(refuel(30, 10_600, 40d, 5d, true));

        FuelStats stats = calculator.calculate(refuels, NOW);

        // 600 km com 60 litros (20 do parcial + 40 do cheio) = 10 km/l.
        assertEquals(10d, stats.averageConsumption, 0.0001d);
        assertEquals(1, stats.measuredSegments);
    }

    @Test
    public void trechoComAbastecimentoNaoRegistrado_eDescartado() {
        List<Refuel> refuels = new ArrayList<>();
        refuels.add(refuel(90, 20_000, 40d, 5d, true));
        Refuel missed = refuel(60, 21_000, 40d, 5d, true);
        missed.missedPrevious = true;
        refuels.add(missed);
        refuels.add(refuel(30, 21_400, 40d, 5d, true));

        FuelStats stats = calculator.calculate(refuels, NOW);

        // Só o último trecho é confiável: 400 km / 40 L.
        assertEquals(10d, stats.averageConsumption, 0.0001d);
        assertEquals(1, stats.measuredSegments);
    }

    @Test
    public void gastoDosUltimos30DiasIgnoraOqueEMaisAntigo() {
        List<Refuel> refuels = new ArrayList<>();
        refuels.add(refuel(90, 30_000, 40d, 5d, true));
        refuels.add(refuel(5, 30_400, 40d, 6d, true));

        FuelStats stats = calculator.calculate(refuels, NOW);

        assertEquals(240d, stats.spentLast30Days, 0.0001d);
        assertEquals(440d, stats.totalSpent, 0.0001d);
        assertEquals(5.5d, stats.averagePricePerLiter, 0.0001d);
    }

    @Test
    public void autonomiaDescontaOqueJaFoiRodado() {
        List<Refuel> refuels = new ArrayList<>();
        refuels.add(refuel(60, 40_000, 40d, 5d, true));
        refuels.add(refuel(30, 40_400, 40d, 5d, true));

        FuelStats stats = calculator.calculate(refuels, NOW);

        // Tanque de 40 L a 10 km/l = 400 km; já rodou 100 desde o último.
        assertEquals(300, stats.estimatedRangeKm(40_500));
        assertTrue(stats.hasConsumption());
    }
}
