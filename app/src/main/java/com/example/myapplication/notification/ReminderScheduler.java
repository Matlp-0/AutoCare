package com.example.myapplication.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Agenda a verificação diária das manutenções.
 *
 * <p>Usa WorkManager: o agendamento é persistido pelo sistema, volta sozinho
 * depois do reboot e respeita Doze. A versão anterior usava
 * {@code AlarmManager.setInexactRepeating}, que se perdia no restart.
 */
public final class ReminderScheduler {

    static final String WORK_NAME = "maintenance_daily_check";

    /** Horário-alvo do aviso; o WorkManager pode deslocar dentro da janela. */
    static final int TARGET_HOUR = 9;

    /** Request code do alarme antigo, mantido só para poder cancelá-lo. */
    private static final int LEGACY_REQUEST_CODE = 2001;

    private ReminderScheduler() {
    }

    public static void scheduleDailyCheck(Context context) {
        Context appContext = context.getApplicationContext();
        cancelLegacyAlarm(appContext);

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                MaintenanceReminderWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMillis(System.currentTimeMillis(), TARGET_HOUR),
                        TimeUnit.MILLISECONDS)
                .build();

        // KEEP: reagendar a cada abertura do app reiniciaria o ciclo e adiaria o
        // aviso para sempre em quem abre o app todo dia.
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, request);
    }

    /**
     * Remove o alarme repetido das versões anteriores. Sem isso quem atualiza o
     * app receberia o aviso duas vezes: pelo alarme antigo e pelo Worker.
     */
    private static void cancelLegacyAlarm(Context appContext) {
        Intent intent = new Intent();
        intent.setClassName(appContext, appContext.getPackageName()
                + ".notification.MaintenanceReminderReceiver");
        PendingIntent pending = PendingIntent.getBroadcast(appContext, LEGACY_REQUEST_CODE, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pending == null) {
            return;
        }
        AlarmManager alarmManager = appContext.getSystemService(AlarmManager.class);
        if (alarmManager != null) {
            alarmManager.cancel(pending);
        }
        pending.cancel();
    }

    /** Milissegundos até a próxima ocorrência de {@code hour}:00 no fuso local. */
    static long initialDelayMillis(long now, int hour) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(now);
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (target.getTimeInMillis() <= now) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }
        return target.getTimeInMillis() - now;
    }
}
