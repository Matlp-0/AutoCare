package com.example.myapplication.domain.scheduler;

import com.example.myapplication.domain.model.MaintenanceStatus;
import com.example.myapplication.domain.model.UpcomingMaintenance;
import com.example.myapplication.domain.model.VehicleHealth;

import java.util.List;

/**
 * Regra simples de saúde do veículo. Isolada de propósito para evoluir depois
 * (peso por criticidade do item, histórico de oficinas, IA etc.).
 */
public class VehicleHealthCalculator {

    private static final int PENALTY_OVERDUE = 15;
    private static final int PENALTY_DUE_SOON = 5;

    public VehicleHealth calculate(MaintenanceScheduler.Result schedule, int maintenanceCount) {
        // Sem plano do fabricante e sem histórico não há o que avaliar: nota otimista
        // aqui seria mentira para o usuário.
        if ((schedule == null || schedule.all.isEmpty()) && maintenanceCount == 0) {
            VehicleHealth unknown = new VehicleHealth(0, "Sem dados");
            unknown.reasons.add("Busque o plano do fabricante ou registre uma manutenção");
            return unknown;
        }

        int score = 100;
        VehicleHealth health = new VehicleHealth(score, "");

        if (schedule != null) {
            for (UpcomingMaintenance item : schedule.all) {
                if (item.status == MaintenanceStatus.OVERDUE) {
                    score -= PENALTY_OVERDUE;
                } else if (item.status == MaintenanceStatus.DUE_SOON) {
                    score -= PENALTY_DUE_SOON;
                }
            }
            if (!schedule.overdue.isEmpty()) {
                health.reasons.add(schedule.overdue.size() + " item(ns) em atraso");
            }
            if (!schedule.dueSoon.isEmpty()) {
                health.reasons.add(schedule.dueSoon.size() + " item(ns) próximos do vencimento");
            }
        }

        // Histórico registrado indica manutenção acompanhada.
        if (maintenanceCount == 0) {
            score -= 10;
            health.reasons.add("Nenhuma manutenção registrada");
        } else {
            score += Math.min(10, maintenanceCount);
            health.reasons.add(maintenanceCount + " manutenção(ões) no histórico");
        }

        score = Math.max(0, Math.min(100, score));
        health.score = score;
        health.label = label(score);
        return health;
    }

    private String label(int score) {
        if (score >= 90) {
            return "Excelente";
        }
        if (score >= 75) {
            return "Bom";
        }
        if (score >= 50) {
            return "Atenção";
        }
        return "Crítico";
    }
}
