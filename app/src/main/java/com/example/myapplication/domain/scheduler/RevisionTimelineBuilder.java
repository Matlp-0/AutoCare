package com.example.myapplication.domain.scheduler;

import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.domain.manual.ManualPlanEntry;
import com.example.myapplication.domain.model.RevisionMilestone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Monta a timeline da tela "Revisões" a partir do plano do fabricante.
 *
 * <p>Nenhum item é inventado: se o plano não cobre um marco diretamente, o marco é
 * mapeado para a revisão equivalente do ciclo (ex.: 90.000 km em um plano que vai
 * até 80.000 repete a revisão de 10.000 km).
 *
 * <p>A marcação do usuário ("realizada" / "não realizada") tem prioridade sobre a
 * dedução automática pelo histórico.
 */
public class RevisionTimelineBuilder {

    private static final int PAST_MILESTONES = 3;
    private static final int FUTURE_MILESTONES = 3;

    public List<RevisionMilestone> build(int currentKm, List<ManualPlanEntry> plan,
                                         List<Maintenance> history) {
        return build(currentKm, plan, history, null);
    }

    /**
     * @param userChecks quilometragem da revisão -> true (usuário marcou como feita)
     *                   ou false (usuário marcou como não feita). Pode ser nulo.
     */
    public List<RevisionMilestone> build(int currentKm, List<ManualPlanEntry> plan,
                                         List<Maintenance> history,
                                         Map<Integer, Boolean> userChecks) {
        List<RevisionMilestone> milestones = new ArrayList<>();
        if (plan == null || plan.isEmpty()) {
            return milestones;
        }

        int base = Integer.MAX_VALUE;
        int cycle = 0;
        for (ManualPlanEntry entry : plan) {
            if (entry.intervalKm > 0) {
                base = Math.min(base, entry.intervalKm);
                cycle = Math.max(cycle, entry.intervalKm);
            }
        }
        if (base == Integer.MAX_VALUE || base <= 0) {
            return milestones;
        }

        int reached = currentKm / base;
        int firstIndex = Math.max(1, reached - PAST_MILESTONES + 1);
        int lastIndex = reached + FUTURE_MILESTONES;
        boolean nextMarked = false;

        for (int index = firstIndex; index <= lastIndex; index++) {
            int km = index * base;
            ManualPlanEntry entry = entryFor(plan, km, cycle);
            if (entry == null) {
                continue;
            }

            RevisionMilestone milestone = new RevisionMilestone();
            milestone.km = km;
            milestone.items = new ArrayList<>(entry.items);
            milestone.labels = new ArrayList<>(entry.labels());
            milestone.description = entry.description;

            Boolean userChoice = userChecks == null ? null : userChecks.get(km);
            milestone.userMarked = userChoice != null;
            boolean executed = userChoice != null
                    ? userChoice
                    : hasMaintenanceAround(history, km, base / 2);

            if (executed) {
                milestone.state = RevisionMilestone.State.DONE;
            } else if (km <= currentKm) {
                // Só cobramos a revisão vencida mais recente; as antigas podem ter
                // sido feitas antes de o usuário começar a usar o app.
                milestone.state = currentKm - km <= base
                        ? RevisionMilestone.State.LATE
                        : RevisionMilestone.State.NO_RECORD;
            } else if (!nextMarked) {
                milestone.state = RevisionMilestone.State.NEXT;
                nextMarked = true;
            } else {
                milestone.state = RevisionMilestone.State.FUTURE;
            }
            milestones.add(milestone);
        }
        return milestones;
    }

    /**
     * Revisão do plano correspondente ao marco: a maior cujo intervalo divide o km.
     * Acima do maior intervalo do plano, o ciclo se repete (90.000 -> 10.000).
     */
    private ManualPlanEntry entryFor(List<ManualPlanEntry> plan, int km, int cycle) {
        ManualPlanEntry direct = findDivisor(plan, km);
        if (direct != null) {
            return direct;
        }
        if (cycle <= 0) {
            return null;
        }
        int equivalent = km % cycle;
        if (equivalent == 0) {
            equivalent = cycle;
        }
        return findDivisor(plan, equivalent);
    }

    private ManualPlanEntry findDivisor(List<ManualPlanEntry> plan, int km) {
        ManualPlanEntry best = null;
        for (ManualPlanEntry entry : plan) {
            if (entry.intervalKm > 0 && km % entry.intervalKm == 0) {
                if (best == null || entry.intervalKm > best.intervalKm) {
                    best = entry;
                }
            }
        }
        return best;
    }

    private boolean hasMaintenanceAround(List<Maintenance> history, int km, int tolerance) {
        if (history == null) {
            return false;
        }
        for (Maintenance maintenance : history) {
            if (Math.abs(maintenance.odometerKm - km) <= tolerance) {
                return true;
            }
        }
        return false;
    }
}
