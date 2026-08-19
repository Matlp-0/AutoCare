package com.example.myapplication.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferências locais simples. Preparado para múltiplos veículos no futuro. */
public class AppPreferences {

    private static final String FILE = "autocare_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_ACTIVE_VEHICLE = "active_vehicle_id";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_LAST_KM_REMINDER = "last_km_reminder_at_";

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public boolean isOnboardingDone() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingDone(boolean done) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply();
    }

    public long getActiveVehicleId() {
        return prefs.getLong(KEY_ACTIVE_VEHICLE, 0L);
    }

    public void setActiveVehicleId(long id) {
        prefs.edit().putLong(KEY_ACTIVE_VEHICLE, id).apply();
    }

    /**
     * Quando o último lembrete de km foi enviado para o veículo; 0 se nunca.
     * O carimbo é por veículo: cada carro tem seu próprio ritmo de uso.
     */
    public long getLastKmReminderAt(long vehicleId) {
        return prefs.getLong(KEY_LAST_KM_REMINDER + vehicleId, 0L);
    }

    public void setLastKmReminderAt(long vehicleId, long millis) {
        prefs.edit().putLong(KEY_LAST_KM_REMINDER + vehicleId, millis).apply();
    }

    /** Modo de aparência: ver constantes de CarbonTheme (0 sistema, 1 claro, 2 escuro). */
    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, 2);
    }

    public void setThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
    }
}
