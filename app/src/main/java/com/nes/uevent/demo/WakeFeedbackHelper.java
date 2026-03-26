package com.nes.uevent.demo;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * 唤醒提示：前台/后台统一用 {@link WakeupPopupActivity}（同一套 UI）；后台额外发通知作兜底。
 */
public final class WakeFeedbackHelper {

    private static final String TAG = "WakeFeedbackHelper";

    static final String CHANNEL_ID = "uevent_wakeup";
    private static final int NOTIFICATION_ID = 0x55667788;

    private WakeFeedbackHelper() {}

    public static void ensureNotificationChannel(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.wakeup_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription(context.getString(R.string.wakeup_notification_channel_desc));
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
    }

    private static boolean hasPostNotificationPermission(Context app) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * @param anyContext Activity 或 Application 均可
     */
    public static void showWakeupFeedback(Context anyContext) {
        if (anyContext == null) {
            return;
        }
        Context app = anyContext.getApplicationContext();
        ensureNotificationChannel(app);

        launchWakeupPopup(app);

        if (UEventDemoApplication.isAppInForeground()) {
            Log.i(TAG, "Foreground: wakeup popup (no notification)");
            return;
        }

        Log.i(TAG, "Background: notification (optional)");
        NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            Log.e(TAG, "NotificationManager null");
            return;
        }

        if (!nm.areNotificationsEnabled()) {
            Log.w(TAG, "App notifications disabled — popup already shown");
            return;
        }
        if (!hasPostNotificationPermission(app)) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted — popup already shown");
            return;
        }

        Intent open = new Intent(app, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap =
                PendingIntent.getActivity(
                        app,
                        0,
                        open,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(app.getString(R.string.wakeup_notification_title))
                .setContentText(app.getString(R.string.wakeup_notification_text))
                .setStyle(
                        new NotificationCompat.BigTextStyle()
                                .bigText(app.getString(R.string.wakeup_notification_text)))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(tap)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false);

        try {
            nm.notify(NOTIFICATION_ID, b.build());
            Log.i(TAG, "Notification posted id=" + NOTIFICATION_ID);
        } catch (Exception e) {
            Log.e(TAG, "notify() failed", e);
            Toast.makeText(app, R.string.wakeup_notification_title, Toast.LENGTH_LONG).show();
        }
    }

    private static void launchWakeupPopup(Context app) {
        try {
            Intent popup = new Intent(app, WakeupPopupActivity.class);
            popup.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            app.startActivity(popup);
            Log.i(TAG, "Wakeup popup launched");
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch WakeupPopupActivity", e);
            Toast.makeText(app, R.string.wakeup_toast_message, Toast.LENGTH_SHORT).show();
        }
    }
}
