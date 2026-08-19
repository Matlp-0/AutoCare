package com.example.myapplication.domain.fuel;

import com.example.myapplication.util.DateUtils;

/**
 * Decide quando cobrar a atualização da quilometragem.
 *
 * <p>Todo prazo em km depende da última leitura do hodômetro: com leitura velha
 * o app subestima o quanto o carro andou e o alerta de manutenção atrasa. Por
 * outro lado, cobrar direto vira ruído — daí o intervalo mínimo entre avisos.
 * Sem dependência de Android — testável.
 */
public class KmReminderPolicy {

    /** A partir daqui a leitura é velha o bastante para atrapalhar as previsões. */
    public static final int STALE_DAYS = 30;

    /** Intervalo mínimo entre dois lembretes. */
    public static final int COOLDOWN_DAYS = 7;

    public static class Decision {
        public boolean shouldNotify;
        public int daysSinceReading;
        /** Quanto o veículo provavelmente rodou desde a leitura; 0 sem ritmo conhecido. */
        public int estimatedKmSince;
    }

    /**
     * @param lastReadingDate data da última leitura (0 quando não há nenhuma)
     * @param lastReminderAt  quando o último lembrete foi enviado (0 se nunca)
     * @param kmPerDay        ritmo de uso conhecido, ou 0
     */
    public Decision decide(long lastReadingDate, long lastReminderAt, float kmPerDay, long now) {
        Decision decision = new Decision();
        if (lastReadingDate <= 0L || lastReadingDate > now) {
            return decision;
        }

        decision.daysSinceReading = DateUtils.daysBetween(lastReadingDate, now);
        if (kmPerDay > 0f) {
            decision.estimatedKmSince = Math.round(decision.daysSinceReading * kmPerDay);
        }
        if (decision.daysSinceReading < STALE_DAYS) {
            return decision;
        }
        if (lastReminderAt > 0L
                && DateUtils.daysBetween(lastReminderAt, now) < COOLDOWN_DAYS) {
            return decision;
        }
        decision.shouldNotify = true;
        return decision;
    }
}
