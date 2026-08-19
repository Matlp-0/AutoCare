package com.example.myapplication.ui.hud;

import android.content.Context;
import android.provider.Settings;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

/**
 * Durações e curva do HUD.
 *
 * <p>Quando o usuário reduz animações no sistema, {@link #duration} devolve 0. Note
 * que {@code ValueAnimator} já multiplica a própria duração pela escala do sistema:
 * por isso aqui a escala só liga/desliga, nunca multiplica de novo — senão o efeito
 * seria aplicado duas vezes. O que realmente depende deste método é o encadeamento
 * por {@code postDelayed}, que o sistema não escala sozinho.
 */
public final class HudMotion {

    public static final long ENTER_PANEL = 220L;
    public static final long ENTER_ODOMETER = 520L;
    public static final long SEGMENT_FILL = 700L;
    public static final long TAG_PULSE = 420L;
    public static final long LOCK_BAR = 170L;

    private HudMotion() {
    }

    /** Curva única do tema: saída rápida, chegada longa. */
    public static Interpolator curve() {
        return new PathInterpolator(0.2f, 0.9f, 0.1f, 1f);
    }

    public static boolean enabled(Context context) {
        if (context == null) {
            return false;
        }
        float scale = Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        return scale > 0f;
    }

    public static long duration(Context context, long base) {
        return enabled(context) ? base : 0L;
    }
}
