package com.example.myapplication.domain.fuel;

import com.example.myapplication.data.local.entity.OdometerReading;
import com.example.myapplication.util.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Calcula o ritmo de uso do veículo (km/dia) a partir das leituras do hodômetro.
 *
 * <p>É o que permite responder "faltam 2.500 km" em dias: sem isso, um prazo em
 * quilometragem nunca vira data e o alerta só aparece quando o usuário lembra de
 * atualizar a km na mão. Sem dependência de Android — testável.
 */
public class UsageEstimator {

    /** Janela considerada: uso antigo não descreve o uso atual. */
    private static final long WINDOW_DAYS = 365L;

    /** Abaixo disso a amostra é curta demais para virar previsão. */
    private static final long MIN_SPAN_DAYS = 14L;

    /** Resultado do cálculo; {@code reliable} diz se dá para projetar datas. */
    public static class Usage {
        public float kmPerDay;
        public int readingCount;
        public long firstDate;
        public long lastDate;
        public int lastKm;
        public int spanKm;
        public int spanDays;
        public boolean reliable;

        /** Média mensal aproximada (30 dias). */
        public int kmPerMonth() {
            return Math.round(kmPerDay * 30f);
        }

        /** Dias até rodar {@code km}; -1 quando não há base para estimar. */
        public int daysFor(int km) {
            if (!reliable || kmPerDay <= 0f) {
                return -1;
            }
            return Math.round(km / kmPerDay);
        }

        /** Data estimada para atingir a quilometragem alvo; 0 quando indisponível. */
        public long dateFor(int currentKm, int targetKm, long now) {
            int missing = targetKm - currentKm;
            if (!reliable || kmPerDay <= 0f || targetKm <= 0) {
                return 0L;
            }
            return now + (long) (missing / kmPerDay * DateUtils.DAY_MILLIS);
        }
    }

    public Usage estimate(List<OdometerReading> readings, long now) {
        Usage usage = new Usage();
        if (readings == null || readings.isEmpty()) {
            return usage;
        }

        List<OdometerReading> sorted = new ArrayList<>(readings);
        Collections.sort(sorted, new Comparator<OdometerReading>() {
            @Override
            public int compare(OdometerReading left, OdometerReading right) {
                int byDate = Long.compare(left.date, right.date);
                return byDate != 0 ? byDate : Integer.compare(left.km, right.km);
            }
        });

        OdometerReading last = sorted.get(sorted.size() - 1);
        usage.lastDate = last.date;
        usage.lastKm = last.km;
        usage.readingCount = sorted.size();

        long windowStart = now - (WINDOW_DAYS * DateUtils.DAY_MILLIS);
        List<OdometerReading> window = new ArrayList<>();
        for (OdometerReading reading : sorted) {
            if (reading.date >= windowStart) {
                window.add(reading);
            }
        }
        // Janela curta demais (ou tudo antigo): usa a série inteira como amostra.
        if (window.size() < 2 || spanDays(window) < MIN_SPAN_DAYS) {
            window = sorted;
        }
        if (window.size() < 2) {
            return usage;
        }

        OdometerReading first = window.get(0);
        OdometerReading latest = window.get(window.size() - 1);
        int spanKm = latest.km - first.km;
        long spanDays = spanDays(window);

        usage.firstDate = first.date;
        usage.spanKm = spanKm;
        usage.spanDays = (int) spanDays;
        if (spanKm <= 0 || spanDays < MIN_SPAN_DAYS) {
            return usage;
        }

        usage.kmPerDay = spanKm / (float) spanDays;
        usage.reliable = usage.kmPerDay > 0f;
        return usage;
    }

    private long spanDays(List<OdometerReading> readings) {
        long from = readings.get(0).date;
        long to = readings.get(readings.size() - 1).date;
        return Math.max(0L, (to - from) / DateUtils.DAY_MILLIS);
    }
}
