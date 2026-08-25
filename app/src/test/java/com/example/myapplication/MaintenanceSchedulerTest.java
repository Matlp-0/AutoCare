package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.domain.manual.ManualPlanEntry;
import com.example.myapplication.domain.model.MaintenanceStatus;
import com.example.myapplication.domain.model.MaintenanceType;
import com.example.myapplication.domain.model.UpcomingMaintenance;
import com.example.myapplication.domain.scheduler.MaintenanceScheduler;
import com.example.myapplication.domain.scheduler.VehicleHealthCalculator;
import com.example.myapplication.util.DateUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MaintenanceSchedulerTest {

    private static final long NOW = 1_776_000_000_000L;

    @Test
    public void oilChangeBecomesDueAfterIntervalKm() {
        Vehicle vehicle = vehicle(147_000);

        Maintenance oil = new Maintenance();
        oil.id = 1;
        oil.vehicleId = 1;
        oil.category = MaintenanceType.OIL_CHANGE.name();
        oil.odometerKm = 145_000;
        oil.date = NOW - 7 * DateUtils.DAY_MILLIS;

        MaintenanceScheduler.Input input = new MaintenanceScheduler.Input();
        input.vehicle = vehicle;
        input.now = NOW;
        input.history = Collections.singletonList(oil);
        input.plan = testPlan();

        MaintenanceScheduler.Result result = new MaintenanceScheduler().calculate(input);

        UpcomingMaintenance oilItem = find(result, MaintenanceType.OIL_CHANGE);
        assertNotNull(oilItem);
        assertEquals(155_000, oilItem.nextDueKm);
        assertEquals(8_000, oilItem.remainingKm);
    }

    @Test
    public void neverExecutedItemIsProjectedToNextMultiple() {
        MaintenanceScheduler.Input input = new MaintenanceScheduler.Input();
        input.vehicle = vehicle(147_000);
        input.now = NOW;
        input.plan = testPlan();

        MaintenanceScheduler.Result result = new MaintenanceScheduler().calculate(input);

        UpcomingMaintenance sparkPlugs = find(result, MaintenanceType.SPARK_PLUGS);
        assertNotNull(sparkPlugs);
        assertEquals(160_000, sparkPlugs.nextDueKm);
    }

    @Test
    public void manualPlanIntervalOverridesCatalogDefault() {
        MaintenanceScheduler.Input input = new MaintenanceScheduler.Input();
        input.vehicle = vehicle(151_000);
        input.now = NOW;
        input.plan = Collections.singletonList(new ManualPlanEntry(
                15_000, 18, "Revisão específica do fabricante", MaintenanceType.OIL_CHANGE));

        MaintenanceScheduler.Result result = new MaintenanceScheduler().calculate(input);

        UpcomingMaintenance oilItem = find(result, MaintenanceType.OIL_CHANGE);
        assertNotNull(oilItem);
        assertEquals(15_000, oilItem.intervalKm);
        assertEquals(18, oilItem.intervalMonths);
        assertEquals(165_000, oilItem.nextDueKm);
    }

    @Test
    public void overdueItemDrivesVehicleStatusAndHealth() {
        Maintenance oil = new Maintenance();
        oil.id = 1;
        oil.vehicleId = 1;
        oil.category = MaintenanceType.OIL_CHANGE.name();
        oil.odometerKm = 120_000;
        oil.date = NOW - 800 * DateUtils.DAY_MILLIS;

        MaintenanceScheduler.Input input = new MaintenanceScheduler.Input();
        input.vehicle = vehicle(147_000);
        input.now = NOW;
        input.history = Collections.singletonList(oil);
        input.plan = testPlan();

        MaintenanceScheduler.Result result = new MaintenanceScheduler().calculate(input);

        assertEquals(MaintenanceStatus.OVERDUE, result.status);
        assertTrue(result.overdue.size() > 0);

        int score = new VehicleHealthCalculator().calculate(result, 1).score;
        assertTrue("saúde deve cair com itens atrasados", score < 100);
    }

    @Test
    public void itemOutsidePlanAndHistoryIsNotScheduled() {
        MaintenanceScheduler.Input input = new MaintenanceScheduler.Input();
        input.vehicle = vehicle(147_000);
        input.now = NOW;
        input.plan = testPlan();

        MaintenanceScheduler.Result result = new MaintenanceScheduler().calculate(input);

        // Bateria não está no plano nem no histórico: o app não inventa a previsão.
        assertNull(find(result, MaintenanceType.BATTERY));
        assertNotNull(find(result, MaintenanceType.SPARK_PLUGS));
    }

    @Test
    public void itemOnlyInHistoryIsScheduledFromUserData() {
        Maintenance battery = new Maintenance();
        battery.id = 1;
        battery.vehicleId = 1;
        battery.category = MaintenanceType.BATTERY.name();
        battery.odometerKm = 140_000;
        battery.date = NOW - 30 * DateUtils.DAY_MILLIS;

        MaintenanceScheduler.Input input = new MaintenanceScheduler.Input();
        input.vehicle = vehicle(147_000);
        input.now = NOW;
        input.history = Collections.singletonList(battery);
        input.plan = testPlan();

        MaintenanceScheduler.Result result = new MaintenanceScheduler().calculate(input);

        UpcomingMaintenance item = find(result, MaintenanceType.BATTERY);
        assertNotNull("registro do usuário deve gerar próxima previsão", item);
        assertEquals(140_000 + MaintenanceType.BATTERY.defaultIntervalKm(), item.nextDueKm);
    }

    @Test
    public void emptyPlanAndEmptyHistoryProducesNothing() {
        MaintenanceScheduler.Input input = new MaintenanceScheduler.Input();
        input.vehicle = vehicle(147_000);
        input.now = NOW;

        MaintenanceScheduler.Result result = new MaintenanceScheduler().calculate(input);

        assertTrue(result.all.isEmpty());
        assertNull(result.next);
    }

    /** Plano de exemplo do TESTE (não existe plano embutido no app). */
    private List<ManualPlanEntry> testPlan() {
        List<ManualPlanEntry> plan = new ArrayList<>();
        plan.add(new ManualPlanEntry(10_000, 12, "Revisão de 10.000 km",
                MaintenanceType.OIL_CHANGE, MaintenanceType.OIL_FILTER));
        plan.add(new ManualPlanEntry(20_000, 24, "Revisão de 20.000 km",
                MaintenanceType.OIL_CHANGE, MaintenanceType.AIR_FILTER));
        plan.add(new ManualPlanEntry(40_000, 36, "Revisão de 40.000 km",
                MaintenanceType.OIL_CHANGE, MaintenanceType.SPARK_PLUGS,
                MaintenanceType.BRAKE_FLUID));
        return plan;
    }

    private UpcomingMaintenance find(MaintenanceScheduler.Result result, MaintenanceType type) {
        for (UpcomingMaintenance item : result.all) {
            if (item.type == type) {
                return item;
            }
        }
        return null;
    }

    private Vehicle vehicle(int km) {
        Vehicle vehicle = new Vehicle();
        vehicle.id = 1;
        vehicle.brand = "Nissan";
        vehicle.model = "Tiida";
        vehicle.engine = "1.8";
        vehicle.year = 2009;
        vehicle.currentKm = km;
        return vehicle;
    }
}
