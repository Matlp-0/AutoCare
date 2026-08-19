package com.example.myapplication.ui.hud;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.domain.model.MaintenanceStatus;

/**
 * Espinha dorsal do tema HUD: o acento não é fixo, ele reflete o estado de
 * manutenção do veículo e reveste a tela inteira.
 *
 * <p>Nenhum componente deve resolver {@code hud_accent_cyan} por conta própria —
 * a cor vem sempre daqui, via {@code applyTheme(StatusTheme)} na tela.
 */
public enum StatusTheme {

    EM_DIA(R.color.hud_accent_cyan, R.string.st_em_dia),
    PROXIMO(R.color.hud_accent_amber, R.string.st_proximo),
    ATRASADO(R.color.hud_accent_red, R.string.st_atrasado);

    public final int colorRes;
    public final int labelRes;

    StatusTheme(int colorRes, int labelRes) {
        this.colorRes = colorRes;
        this.labelRes = labelRes;
    }

    /**
     * Classificação direta a partir de km, útil para pré-visualizar um item isolado
     * e para teste. A tela usa {@link #from(MaintenanceStatus)}.
     */
    public static StatusTheme from(int kmRestantes, int intervalo) {
        if (kmRestantes <= 0) {
            return ATRASADO;
        }
        return kmRestantes <= intervalo * 0.2f ? PROXIMO : EM_DIA;
    }

    /**
     * Fonte da verdade da tela.
     *
     * <p>Quem decide o estado é o {@code MaintenanceScheduler}, que já pondera km e
     * prazo em meses. Traduzir daqui — em vez de recalcular na UI — garante que o
     * acento global e o estado de cada item nunca discordem.
     *
     * <p>{@code UNKNOWN} (veículo recém-cadastrado, sem plano nem histórico) usa o
     * ciano: pintar a tela de cinza logo na primeira abertura mataria justamente a
     * sequência de entrada, e o bloco "Comece aqui" já explica o que falta.
     */
    public static StatusTheme from(MaintenanceStatus status) {
        if (status == null) {
            return EM_DIA;
        }
        switch (status) {
            case OVERDUE:
                return ATRASADO;
            case DUE_SOON:
                return PROXIMO;
            default:
                return EM_DIA;
        }
    }

    public int color(Context context) {
        return ContextCompat.getColor(context, colorRes);
    }

    public String label(Context context) {
        return context.getString(labelRes);
    }
}
