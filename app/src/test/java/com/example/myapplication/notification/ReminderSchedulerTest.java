package com.example.myapplication.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.myapplication.util.DateUtils;

import org.junit.Test;

import java.util.Calendar;

public class ReminderSchedulerTest {

    private long at(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2026, Calendar.MARCH, 10, hour, minute, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @Test
    public void antesDoHorario_esperaAteHoje() {
        long delay = ReminderScheduler.initialDelayMillis(at(7, 0), 9);

        assertEquals(2 * 60 * 60 * 1000L, delay);
    }

    @Test
    public void depoisDoHorario_vaiParaAmanha() {
        long delay = ReminderScheduler.initialDelayMillis(at(10, 0), 9);

        assertEquals(23 * 60 * 60 * 1000L, delay);
    }

    @Test
    public void noHorarioExato_naoAgendaParaTras() {
        long delay = ReminderScheduler.initialDelayMillis(at(9, 0), 9);

        assertEquals(DateUtils.DAY_MILLIS, delay);
    }

    @Test
    public void atrasoNuncaEhNegativo() {
        for (int hour = 0; hour < 24; hour++) {
            long delay = ReminderScheduler.initialDelayMillis(at(hour, 30), 9);

            assertTrue("hora " + hour, delay > 0 && delay <= DateUtils.DAY_MILLIS);
        }
    }
}
