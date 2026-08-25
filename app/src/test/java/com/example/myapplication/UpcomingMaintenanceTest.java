package com.example.myapplication;

import static org.junit.Assert.assertEquals;

import com.example.myapplication.domain.model.UpcomingMaintenance;

import org.junit.Test;

public class UpcomingMaintenanceTest {

    @Test
    public void remainingText_prefersKilometers_whenKilometerDeadlineIsMoreUrgent() {
        UpcomingMaintenance maintenance = new UpcomingMaintenance();
        maintenance.intervalKm = 10_000;
        maintenance.intervalMonths = 12;
        maintenance.remainingKm = 1_500;
        maintenance.remainingDays = 300;

        assertEquals("1.500 km", maintenance.remainingText());
    }

    @Test
    public void remainingText_prefersTime_whenTimeDeadlineIsMoreUrgent() {
        UpcomingMaintenance maintenance = new UpcomingMaintenance();
        maintenance.intervalKm = 10_000;
        maintenance.intervalMonths = 12;
        maintenance.remainingKm = 9_000;
        maintenance.remainingDays = 30;

        assertEquals("Em aproximadamente 30 dias", maintenance.remainingText());
    }

    @Test
    public void remainingText_reportsOverdueKilometers() {
        UpcomingMaintenance maintenance = new UpcomingMaintenance();
        maintenance.intervalKm = 10_000;
        maintenance.intervalMonths = 0;
        maintenance.remainingKm = -1_200;
        maintenance.remainingDays = 0;

        assertEquals("Atrasado 1.200 km", maintenance.remainingText());
    }

    @Test
    public void remainingDetailed_addsUsagePace_whenKilometersDriveDeadline() {
        UpcomingMaintenance maintenance = new UpcomingMaintenance();
        maintenance.intervalKm = 10_000;
        maintenance.intervalMonths = 12;
        maintenance.remainingKm = 2_000;
        maintenance.remainingDays = 300;
        maintenance.kmPaceDays = 60;

        assertEquals("2.000 km · ≈ 60 dias", maintenance.remainingDetailed());
    }
}
