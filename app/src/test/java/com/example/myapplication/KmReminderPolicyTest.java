package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.myapplication.domain.fuel.KmReminderPolicy;
import com.example.myapplication.util.DateUtils;

import org.junit.Test;

public class KmReminderPolicyTest {

    private static final long NOW = 1_700_000_000_000L;

    private final KmReminderPolicy policy = new KmReminderPolicy();

    private long daysAgo(int days) {
        return NOW - (days * DateUtils.DAY_MILLIS);
    }

    @Test
    public void leituraRecente_naoIncomoda() {
        KmReminderPolicy.Decision decision = policy.decide(daysAgo(10), 0L, 30f, NOW);

        assertFalse(decision.shouldNotify);
        assertEquals(10, decision.daysSinceReading);
    }

    @Test
    public void leituraVelha_cobraEEstimaKmRodado() {
        KmReminderPolicy.Decision decision = policy.decide(daysAgo(40), 0L, 30f, NOW);

        assertTrue(decision.shouldNotify);
        assertEquals(40, decision.daysSinceReading);
        assertEquals(1_200, decision.estimatedKmSince);
    }

    @Test
    public void semRitmoConhecido_aindaCobraSemEstimativa() {
        KmReminderPolicy.Decision decision = policy.decide(daysAgo(45), 0L, 0f, NOW);

        assertTrue(decision.shouldNotify);
        assertEquals(0, decision.estimatedKmSince);
    }

    @Test
    public void lembreteRecente_respeitaIntervalo() {
        KmReminderPolicy.Decision decision = policy.decide(daysAgo(60), daysAgo(3), 30f, NOW);

        assertFalse(decision.shouldNotify);
    }

    @Test
    public void lembreteAntigo_podeCobrarDeNovo() {
        KmReminderPolicy.Decision decision = policy.decide(daysAgo(60), daysAgo(10), 30f, NOW);

        assertTrue(decision.shouldNotify);
    }

    @Test
    public void semLeituraNenhuma_naoCobra() {
        KmReminderPolicy.Decision decision = policy.decide(0L, 0L, 30f, NOW);

        assertFalse(decision.shouldNotify);
    }
}
