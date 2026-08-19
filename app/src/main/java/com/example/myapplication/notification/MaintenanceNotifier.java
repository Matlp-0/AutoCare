package com.example.myapplication.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.myapplication.R;
import com.example.myapplication.domain.model.MaintenanceStatus;
import com.example.myapplication.domain.model.UpcomingMaintenance;
import com.example.myapplication.ui.main.MainActivity;
import com.example.myapplication.util.Formatters;

import java.util.List;

/** Alertas de manutenção usando as APIs nativas de notificação. */
public class MaintenanceNotifier {

    public static final String CHANNEL_ID = "maintenance_alerts";
    private static final int NOTIFICATION_ID = 1001;
    private static final int KM_NOTIFICATION_ID = 1002;
    /** Faixas separadas para não haver colisão entre veículos e tipos de aviso. */
    private static final int MAINTENANCE_BASE_ID = 100_000;
    private static final int KM_BASE_ID = 200_000;

    private final Context context;

    public MaintenanceNotifier(Context context) {
        this.context = context.getApplicationContext();
    }

    public void createChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.notification_channel_description));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /** Notifica o item mais urgente, se houver algo atrasado ou próximo. */
    public void notifyUpcoming(List<UpcomingMaintenance> items) {
        notifyUpcoming(items, null, 0L);
    }

    /**
     * Versão por veículo: cada carro tem sua própria notificação (ids distintos)
     * e o nome entra no texto quando há mais de um na garagem.
     */
    public void notifyUpcoming(List<UpcomingMaintenance> items, String vehicleName,
                               long vehicleId) {
        if (items == null || items.isEmpty() || !hasPermission()) {
            return;
        }
        UpcomingMaintenance item = items.get(0);
        if (item.status == MaintenanceStatus.OK) {
            return;
        }
        String title = item.status == MaintenanceStatus.OVERDUE
                ? "Uma manutenção está atrasada"
                : "Está chegando a hora: " + item.label;
        if (vehicleName != null) {
            title = vehicleName + " • " + title;
        }
        String message = item.label + " • " + item.remainingText();
        notify(title, message, MAINTENANCE_BASE_ID + (int) vehicleId, null);
    }

    /**
     * Cobra a atualização da quilometragem. Ao tocar, o app abre direto o diálogo
     * de km — o lembrete não serve de nada se ainda exigir procurar o botão.
     */
    public void notifyKmUpdate(int daysSinceReading, int estimatedKmSince, String vehicleName,
                               long vehicleId) {
        String message = estimatedKmSince > 0
                ? context.getString(R.string.notification_km_message_pace, daysSinceReading,
                        Formatters.km(estimatedKmSince))
                : context.getString(R.string.notification_km_message, daysSinceReading);
        String title = context.getString(R.string.notification_km_title);
        if (vehicleName != null) {
            title = vehicleName + " • " + title;
        }
        int notificationId = vehicleId > 0 ? KM_BASE_ID + (int) vehicleId : KM_NOTIFICATION_ID;
        notify(title, message, notificationId, MainActivity.ACTION_UPDATE_KM);
    }

    public void notify(String title, String message) {
        notify(title, message, NOTIFICATION_ID, null);
    }

    private void notify(String title, String message, int notificationId, String action) {
        if (!hasPermission()) {
            return;
        }
        Intent intent = new Intent(context, MainActivity.class);
        if (action != null) {
            intent.putExtra(MainActivity.EXTRA_ACTION, action);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    public boolean hasPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }
}
