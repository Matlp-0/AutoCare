package com.example.myapplication.ui.carbon;

import android.app.Activity;
import android.view.View;
import android.view.Window;

import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.myapplication.R;

/**
 * Barras do sistema alinhadas ao Carbon UI.
 *
 * <p>A faixa da status bar é pintada pelo root da Activity; aqui ela recebe o
 * fundo do HUD (que já é claro ou escuro conforme o tema) e os ícones ganham o
 * contraste correspondente.
 */
public final class CarbonSystemBars {

    private CarbonSystemBars() {
    }

    public static void apply(Activity activity) {
        if (activity == null || activity.getWindow() == null) {
            return;
        }
        boolean night = CarbonTheme.isNight(activity);
        Window window = activity.getWindow();
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(!night);
        controller.setAppearanceLightNavigationBars(!night);

        View mainRoot = activity.findViewById(R.id.mainRoot);
        if (mainRoot != null) {
            mainRoot.setBackgroundColor(ContextCompat.getColor(activity, R.color.carbon_bg));
        }
    }

    /** Aplica depois do layout: o tema reaplica a aparência padrão durante o resume. */
    public static void applyOnResume(final Activity activity, View root) {
        if (root == null) {
            apply(activity);
            return;
        }
        root.post(new Runnable() {
            @Override
            public void run() {
                apply(activity);
            }
        });
    }
}
