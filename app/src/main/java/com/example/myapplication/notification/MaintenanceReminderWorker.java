package com.example.myapplication.notification;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapplication.AutoCareApp;

/**
 * Verificação diária das manutenções.
 *
 * <p>Roda no WorkManager e não no AlarmManager porque o trabalho precisa
 * sobreviver a reboot e a Doze: com alarme, o agendamento morria no restart e os
 * lembretes só voltavam quando o usuário abria o app.
 */
public class MaintenanceReminderWorker extends Worker {

    public MaintenanceReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            new ReminderChecker().run(AutoCareApp.container(getApplicationContext()),
                    System.currentTimeMillis());
            return Result.success();
        } catch (RuntimeException error) {
            // Falha pontual (banco ocupado, plano indisponível): tenta de novo.
            return Result.retry();
        }
    }
}
