package com.example.myapplication.domain.scheduler;

import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.data.local.entity.MaintenanceItem;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.domain.manual.ManualPlanEntry;
import com.example.myapplication.domain.model.MaintenanceStatus;
import com.example.myapplication.domain.model.MaintenanceType;
import com.example.myapplication.domain.model.UpcomingMaintenance;
import com.example.myapplication.util.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Motor de manutenção: cruza quilometragem atual, data, histórico e plano do
 * fabricante para dizer o que precisa ser feito. Sem dependência de Android,
 * portanto testável e reaproveitável.
 */
public class MaintenanceScheduler {

    /** Quantos dias antes do vencimento já consideramos "atenção". */
    private static final int DUE_SOON_DAYS = 60;

    public static class Input {
        public Vehicle vehicle;
        public long now = System.currentTimeMillis();
        public List<Maintenance> history = new ArrayList<>();
        public List<MaintenanceItem> items = new ArrayList<>();
        public List<ManualPlanEntry> plan = new ArrayList<>();
        /** Ritmo de uso (km/dia); 0 quando não há leituras suficientes. */
        public float kmPerDay;
    }

    public static class Result {
        public List<UpcomingMaintenance> all = new ArrayList<>();
        public List<UpcomingMaintenance> overdue = new ArrayList<>();
        public List<UpcomingMaintenance> dueSoon = new ArrayList<>();
        public MaintenanceStatus status = MaintenanceStatus.UNKNOWN;
        public UpcomingMaintenance next;
    }

    public Result calculate(Input input) {
        Result result = new Result();
        if (input == null || input.vehicle == null) {
            return result;
        }

        int currentKm = Math.max(0, input.vehicle.currentKm);
        long now = input.now;

        Map<MaintenanceType, Maintenance> lastByType = lastExecutionByType(input.history, input.items);
        Map<MaintenanceType, int[]> intervals = buildIntervals(input.plan, lastByType.keySet());

        for (Map.Entry<MaintenanceType, int[]> entry : intervals.entrySet()) {
            MaintenanceType type = entry.getKey();
            int intervalKm = entry.getValue()[0];
            int intervalMonths = entry.getValue()[1];
            if (intervalKm <= 0 && intervalMonths <= 0) {
                continue;
            }

            UpcomingMaintenance item = new UpcomingMaintenance();
            item.type = type;
            item.label = type.label();
            item.intervalKm = intervalKm;
            item.intervalMonths = intervalMonths;

            Maintenance last = lastByType.get(type);
            if (last != null) {
                item.lastDoneKm = last.odometerKm;
                item.lastDoneDate = last.date;
                item.nextDueKm = intervalKm > 0 ? last.odometerKm + intervalKm : 0;
                item.nextDueDate = intervalMonths > 0 ? DateUtils.addMonths(last.date, intervalMonths) : 0;
            } else {
                // Sem histórico: projeta o próximo múltiplo do intervalo acima da km atual.
                item.nextDueKm = intervalKm > 0 ? nextMultiple(currentKm, intervalKm) : 0;
                item.nextDueDate = 0;
            }

            item.remainingKm = item.nextDueKm > 0 ? item.nextDueKm - currentKm : Integer.MAX_VALUE;
            item.remainingDays = item.nextDueDate > 0
                    ? DateUtils.daysBetween(now, item.nextDueDate)
                    : Integer.MAX_VALUE;
            // Com o ritmo de uso conhecido, o prazo em km também vira data.
            if (input.kmPerDay > 0f && item.nextDueKm > 0) {
                item.kmPaceDays = Math.round(item.remainingKm / input.kmPerDay);
                item.kmPaceDate = now + (long) (item.kmPaceDays * (double) DateUtils.DAY_MILLIS);
            }
            item.status = resolveStatus(item);

            result.all.add(item);
            if (item.status == MaintenanceStatus.OVERDUE) {
                result.overdue.add(item);
            } else if (item.status == MaintenanceStatus.DUE_SOON) {
                result.dueSoon.add(item);
            }
        }

        Collections.sort(result.all, urgencyComparator());
        Collections.sort(result.overdue, urgencyComparator());
        Collections.sort(result.dueSoon, urgencyComparator());

        for (UpcomingMaintenance item : result.all) {
            result.status = MaintenanceStatus.worst(result.status, item.status);
        }
        if (!result.all.isEmpty()) {
            result.next = result.all.get(0);
        }
        return result;
    }

    /**
     * Intervalo (km, meses) por item. Só entram no cronograma itens que o plano do
     * fabricante cita ou que o usuário já registrou no histórico — o app não sugere
     * manutenção sem base no veículo real. O intervalo do plano tem prioridade
     * sobre o padrão do item.
     */
    private Map<MaintenanceType, int[]> buildIntervals(List<ManualPlanEntry> plan,
                                                       Set<MaintenanceType> historyTypes) {
        Map<MaintenanceType, int[]> intervals = new EnumMap<>(MaintenanceType.class);
        Map<MaintenanceType, int[]> planIntervals = new EnumMap<>(MaintenanceType.class);
        Set<MaintenanceType> types = new LinkedHashSet<>();
        if (historyTypes != null) {
            types.addAll(historyTypes);
        }

        if (plan != null) {
            for (ManualPlanEntry entry : plan) {
                types.addAll(entry.items);
            }
        }
        for (MaintenanceType type : types) {
            if (!type.isScheduled()) {
                continue;
            }
            intervals.put(type, new int[]{type.defaultIntervalKm(), type.defaultIntervalMonths()});
        }

        if (plan != null) {
            for (ManualPlanEntry entry : plan) {
                for (MaintenanceType type : entry.items) {
                    int[] candidate = new int[]{entry.intervalKm, entry.intervalMonths};
                    int[] current = planIntervals.get(type);
                    if (current == null || isMoreFrequent(candidate, current)) {
                        planIntervals.put(type, candidate);
                    }
                }
            }
        }

        // Um intervalo explícito no manual substitui o fallback do catálogo,
        // mesmo quando o manual usa uma periodicidade maior que o padrão.
        for (Map.Entry<MaintenanceType, int[]> entry : planIntervals.entrySet()) {
            intervals.put(entry.getKey(), entry.getValue());
        }
        return intervals;
    }

    private boolean isMoreFrequent(int[] candidate, int[] current) {
        if (candidate[0] > 0 && current[0] <= 0) {
            return true;
        }
        if (candidate[0] > 0 && current[0] > 0) {
            if (candidate[0] != current[0]) {
                return candidate[0] < current[0];
            }
            return candidate[1] > 0 && (current[1] <= 0 || candidate[1] < current[1]);
        }
        if (candidate[0] <= 0 && current[0] > 0) {
            return false;
        }
        return candidate[1] > 0 && (current[1] <= 0 || candidate[1] < current[1]);
    }

    private Map<MaintenanceType, Maintenance> lastExecutionByType(List<Maintenance> history,
                                                                 List<MaintenanceItem> items) {
        Map<MaintenanceType, Maintenance> last = new EnumMap<>(MaintenanceType.class);
        if (history == null) {
            return last;
        }
        Map<Long, Maintenance> byId = new java.util.HashMap<>();
        for (Maintenance maintenance : history) {
            byId.put(maintenance.id, maintenance);
            register(last, MaintenanceType.fromName(maintenance.category), maintenance);
        }
        if (items != null) {
            for (MaintenanceItem item : items) {
                Maintenance parent = byId.get(item.maintenanceId);
                if (parent != null) {
                    register(last, MaintenanceType.fromName(item.type), parent);
                }
            }
        }
        return last;
    }

    private void register(Map<MaintenanceType, Maintenance> last, MaintenanceType type,
                          Maintenance maintenance) {
        if (type == MaintenanceType.OTHER) {
            return;
        }
        Maintenance current = last.get(type);
        if (current == null || maintenance.date > current.date
                || (maintenance.date == current.date && maintenance.odometerKm > current.odometerKm)) {
            last.put(type, maintenance);
        }
    }

    private MaintenanceStatus resolveStatus(UpcomingMaintenance item) {
        MaintenanceStatus status = MaintenanceStatus.OK;
        if (item.nextDueKm > 0) {
            int threshold = Math.max(500, Math.round(item.intervalKm * 0.15f));
            if (item.remainingKm < 0) {
                status = MaintenanceStatus.OVERDUE;
            } else if (item.remainingKm <= threshold) {
                status = MaintenanceStatus.DUE_SOON;
            }
        }
        if (item.nextDueDate > 0) {
            if (item.remainingDays < 0) {
                status = MaintenanceStatus.worst(status, MaintenanceStatus.OVERDUE);
            } else if (item.remainingDays <= DUE_SOON_DAYS) {
                status = MaintenanceStatus.worst(status, MaintenanceStatus.DUE_SOON);
            }
        }
        return status;
    }

    private Comparator<UpcomingMaintenance> urgencyComparator() {
        return new Comparator<UpcomingMaintenance>() {
            @Override
            public int compare(UpcomingMaintenance a, UpcomingMaintenance b) {
                return Float.compare(urgency(a), urgency(b));
            }
        };
    }

    /** Quanto menor, mais urgente (fração do intervalo restante). */
    private float urgency(UpcomingMaintenance item) {
        float kmRatio = Float.MAX_VALUE;
        float timeRatio = Float.MAX_VALUE;
        if (item.intervalKm > 0 && item.remainingKm != Integer.MAX_VALUE) {
            kmRatio = item.remainingKm / (float) item.intervalKm;
        }
        if (item.intervalMonths > 0 && item.remainingDays != Integer.MAX_VALUE) {
            timeRatio = item.remainingDays / (item.intervalMonths * 30f);
        }
        return Math.min(kmRatio, timeRatio);
    }

    private int nextMultiple(int currentKm, int interval) {
        int multiples = currentKm / interval;
        return (multiples + 1) * interval;
    }
}
