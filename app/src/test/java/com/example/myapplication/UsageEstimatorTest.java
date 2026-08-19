package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.myapplication.data.local.entity.OdometerReading;
import com.example.myapplication.domain.fuel.UsageEstimator;
import com.example.myapplication.util.DateUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class UsageEstimatorTest {

    private static final long NOW = 1_700_000_000_000L;

    private final UsageEstimator estimator = new UsageEstimator();

    private OdometerReading reading(long daysAgo, int km) {
        return OdometerReading.of(1L, NOW - (daysAgo * DateUtils.DAY_MILLIS), km,
                OdometerReading.SOURCE_MANUAL);
    }

    @Test
    public void umaLeituraSo_naoProjeta() {
        List<OdometerReading> readings = new ArrayList<>();
        readings.add(reading(10, 1_000));

        UsageEstimator.Usage usage = estimator.estimate(readings, NOW);

        assertFalse(usage.reliable);
        assertEquals(1_000, usage.lastKm);
    }

    @Test
    public void amostraCurtaDemais_naoProjeta() {
        List<OdometerReading> readings = new ArrayList<>();
        readings.add(reading(5, 1_000));
        readings.add(reading(0, 1_300));

        UsageEstimator.Usage usage = estimator.estimate(readings, NOW);

        assertFalse(usage.reliable);
    }

    @Test
    public void calculaKmPorDiaNaJanela() {
        List<OdometerReading> readings = new ArrayList<>();
        readings.add(reading(100, 10_000));
        readings.add(reading(50, 11_500));
        readings.add(reading(0, 13_000));

        UsageEstimator.Usage usage = estimator.estimate(readings, NOW);

        assertTrue(usage.reliable);
        assertEquals(30f, usage.kmPerDay, 0.01f);
        assertEquals(900, usage.kmPerMonth());
        assertEquals(13_000, usage.lastKm);
    }

    @Test
    public void projetaDataParaAKmAlvo() {
        List<OdometerReading> readings = new ArrayList<>();
        readings.add(reading(100, 10_000));
        readings.add(reading(0, 13_000));

        UsageEstimator.Usage usage = estimator.estimate(readings, NOW);

        // 30 km/dia; faltam 600 km para 13.600.
        assertEquals(20, usage.daysFor(600));
        assertEquals(NOW + 20 * DateUtils.DAY_MILLIS, usage.dateFor(13_000, 13_600, NOW), 1000d);
    }

    @Test
    public void hodometroSemAvanco_naoProjeta() {
        List<OdometerReading> readings = new ArrayList<>();
        readings.add(reading(100, 10_000));
        readings.add(reading(0, 10_000));

        UsageEstimator.Usage usage = estimator.estimate(readings, NOW);

        assertFalse(usage.reliable);
        assertEquals(0f, usage.kmPerDay, 0.0001f);
    }
}
