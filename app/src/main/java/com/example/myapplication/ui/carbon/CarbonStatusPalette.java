package com.example.myapplication.ui.carbon;

import com.example.myapplication.R;
import com.example.myapplication.domain.model.MaintenanceStatus;

/**
 * Cores do HUD para cada estado de manutenção.
 *
 * <p>Fonte única do mapeamento estado -> cor desde que todas as telas migraram
 * para o Carbon UI.
 *
 * <p>Amarelo ácido só aparece em atenção/seleção; nunca como cor de rotina.
 */
public final class CarbonStatusPalette {

    private CarbonStatusPalette() {
    }

    public static int colorRes(MaintenanceStatus status) {
        if (status == null) {
            return R.color.carbon_text_secondary;
        }
        switch (status) {
            case OVERDUE:
                return R.color.carbon_alert;
            case DUE_SOON:
                return R.color.carbon_accent;
            case OK:
                return R.color.carbon_cyan;
            default:
                return R.color.carbon_text_secondary;
        }
    }

    /** Rótulo curto usado nas linhas da lista. */
    public static int shortLabelRes(MaintenanceStatus status) {
        if (status == null) {
            return R.string.carbon_status_unknown;
        }
        switch (status) {
            case OVERDUE:
                return R.string.carbon_status_late;
            case DUE_SOON:
                return R.string.carbon_status_soon;
            case OK:
                return R.string.carbon_status_ok;
            default:
                return R.string.carbon_status_unknown;
        }
    }

    /** Cor da barra de saúde conforme a nota. */
    public static int healthColorRes(int score) {
        if (score <= 0) {
            return R.color.carbon_text_dim;
        }
        if (score >= 75) {
            return R.color.carbon_cyan;
        }
        if (score >= 50) {
            return R.color.carbon_accent;
        }
        return R.color.carbon_alert;
    }
}
