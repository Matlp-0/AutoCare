package com.example.myapplication.domain.model;

import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.Formatters;

/** Item calculado pelo MaintenanceScheduler (não é persistido diretamente). */
public class UpcomingMaintenance {

    public MaintenanceType type;
    public String label;
    public int intervalKm;
    public int intervalMonths;
    public int lastDoneKm = -1;
    public long lastDoneDate;
    public int nextDueKm;
    public long nextDueDate;
    /** Positivo = faltam km; negativo = passou do ponto. */
    public int remainingKm;
    public int remainingDays;
    /** Dias estimados até a km de vencimento, pelo ritmo de uso; -1 sem base. */
    public int kmPaceDays = -1;
    /** Data estimada de vencimento pela km; 0 quando não há ritmo confiável. */
    public long kmPaceDate;
    public MaintenanceStatus status = MaintenanceStatus.OK;

    /** Texto curto para a Home: "2.500 km" ou "Em aproximadamente 2 meses". */
    public String remainingText() {
        boolean kmDrivesTheDeadline = Math.abs(remainingKm) <= Math.max(intervalKm, 1)
                && (intervalKm > 0);
        if (kmDrivesTheDeadline && (intervalMonths == 0 || kmIsMoreUrgent())) {
            return remainingKm >= 0
                    ? Formatters.km(remainingKm)
                    : "Atrasado " + Formatters.km(-remainingKm);
        }
        return DateUtils.humanizeDays(remainingDays);
    }

    /**
     * Tradução do prazo em km para tempo, usando o ritmo de uso do veículo:
     * "≈ 3 meses". Vazio quando ainda não há leituras suficientes.
     */
    public String paceText() {
        if (kmPaceDays < 0) {
            return "";
        }
        return "≈ " + DateUtils.humanizeDays(kmPaceDays).replace("Em aproximadamente ", "")
                .replace("Atrasado há ", "atrasado ");
    }

    /**
     * Prazo pronto para a tela: "2.500 km · ≈ 2 meses" quando o vencimento é por
     * quilometragem e o ritmo de uso é conhecido; caso contrário só o prazo base.
     */
    public String remainingDetailed() {
        String base = remainingText();
        String pace = paceText();
        if (pace.isEmpty() || !base.contains("km")) {
            return base;
        }
        return base + " · " + pace;
    }

    private boolean kmIsMoreUrgent() {
        if (intervalKm <= 0) {
            return false;
        }
        if (intervalMonths <= 0) {
            return true;
        }
        float kmRatio = remainingKm / (float) intervalKm;
        float timeRatio = remainingDays / (intervalMonths * 30f);
        return kmRatio <= timeRatio;
    }
}
