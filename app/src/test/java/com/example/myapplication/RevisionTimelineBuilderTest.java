package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.domain.manual.ManualPlanEntry;
import com.example.myapplication.domain.model.MaintenanceType;
import com.example.myapplication.domain.model.RevisionMilestone;
import com.example.myapplication.domain.scheduler.RevisionTimelineBuilder;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RevisionTimelineBuilderTest {

    private final RevisionTimelineBuilder builder = new RevisionTimelineBuilder();

    /** Plano de 10k a 30k, como o baixado do fabricante. */
    private List<ManualPlanEntry> plan() {
        List<ManualPlanEntry> plan = new ArrayList<>();
        plan.add(new ManualPlanEntry(10_000, 12, "Revisão de 10.000 km",
                MaintenanceType.OIL_CHANGE));
        plan.add(new ManualPlanEntry(20_000, 24, "Revisão de 20.000 km",
                MaintenanceType.OIL_CHANGE, MaintenanceType.AIR_FILTER));
        plan.add(new ManualPlanEntry(30_000, 36, "Revisão de 30.000 km",
                MaintenanceType.OIL_CHANGE, MaintenanceType.SPARK_PLUGS));
        return plan;
    }

    @Test
    public void milestonesBeyondPlanReuseRealPlanItems() {
        // 88.000 km: os marcos passam de 30.000, o maior do plano.
        List<RevisionMilestone> timeline = builder.build(88_000, plan(), null);

        assertFalse(timeline.isEmpty());
        for (RevisionMilestone milestone : timeline) {
            assertFalse("nenhum marco pode ficar sem itens", milestone.labels.isEmpty());
            for (String label : milestone.labels) {
                assertTrue("item precisa vir do plano real: " + label,
                        label.equals(MaintenanceType.OIL_CHANGE.label())
                                || label.equals(MaintenanceType.AIR_FILTER.label())
                                || label.equals(MaintenanceType.SPARK_PLUGS.label()));
            }
        }

        // 90.000 = 3 ciclos de 30.000 -> equivale à revisão de 30.000 km.
        RevisionMilestone at90 = find(timeline, 90_000);
        assertNotNull(at90);
        assertTrue(at90.items.contains(MaintenanceType.SPARK_PLUGS));
    }

    @Test
    public void userMarkOverridesHistoryDetection() {
        Maintenance maintenance = new Maintenance();
        maintenance.odometerKm = 40_000;

        Map<Integer, Boolean> checks = new HashMap<>();
        checks.put(40_000, false);

        List<RevisionMilestone> timeline = builder.build(45_000, plan(),
                Collections.singletonList(maintenance), checks);

        RevisionMilestone at40 = find(timeline, 40_000);
        assertNotNull(at40);
        // O histórico sugeriria "realizada", mas o usuário disse que não fez.
        assertEquals(RevisionMilestone.State.LATE, at40.state);
        assertTrue(at40.userMarked);
    }

    @Test
    public void userCanMarkFutureRevisionAsDone() {
        Map<Integer, Boolean> checks = new HashMap<>();
        checks.put(50_000, true);

        List<RevisionMilestone> timeline = builder.build(45_000, plan(), null, checks);

        RevisionMilestone at50 = find(timeline, 50_000);
        assertNotNull(at50);
        assertEquals(RevisionMilestone.State.DONE, at50.state);
        assertTrue(at50.userMarked);
    }

    @Test
    public void emptyPlanProducesEmptyTimeline() {
        assertTrue(builder.build(50_000, null, null).isEmpty());
        assertTrue(builder.build(50_000, new ArrayList<ManualPlanEntry>(), null).isEmpty());
    }

    private RevisionMilestone find(List<RevisionMilestone> timeline, int km) {
        for (RevisionMilestone milestone : timeline) {
            if (milestone.km == km) {
                return milestone;
            }
        }
        return null;
    }
}
