package com.example.myapplication.ui.carbon;

import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Modo claro/escuro do Carbon UI.
 *
 * <p>As duas paletas vivem em {@code values/carbon_colors.xml} (claro) e
 * {@code values-night/carbon_colors.xml} (escuro): trocar o modo noturno troca o
 * tema inteiro do app, inclusive as telas que ainda usam Material.
 */
public final class CarbonTheme {

    public static final int MODE_SYSTEM = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    private CarbonTheme() {
    }

    public static void apply(int mode) {
        AppCompatDelegate.setDefaultNightMode(toDelegateMode(mode));
    }

    private static int toDelegateMode(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case MODE_DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    /** true quando a configuração atual está no modo escuro. */
    public static boolean isNight(Context context) {
        if (context == null) {
            return true;
        }
        int nightFlags = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}
