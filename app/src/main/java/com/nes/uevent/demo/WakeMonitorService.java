package com.nes.uevent.demo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Keep process alive in background so UEvent observer remains active.
 */
public class WakeMonitorService extends Service {

    private static final String CHANNEL_ID = "uevent_service";
    private static final int FOREGROUND_ID = 0x2299AA11;

    @Override
    public void onCreate() {
        super.onCreate();
        ensureServiceChannel();
        Notification n =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(getString(R.string.service_notification_title))
                        .setContentText(getString(R.string.service_notification_text))
                        .setOngoing(true)
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .build();
        startForeground(FOREGROUND_ID, n);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void ensureServiceChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.service_channel_name),
                        NotificationManager.IMPORTANCE_MIN);
        channel.setDescription(getString(R.string.service_channel_desc));
        nm.createNotificationChannel(channel);
    }
}
