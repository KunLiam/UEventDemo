package com.nes.uevent.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_POST_NOTIFICATIONS = 1001;

    private TextView textLastWakeup;
    private TextView textNotifyStatus;
    private UEventMonitor monitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WakeFeedbackHelper.ensureNotificationChannel(getApplicationContext());
        setContentView(R.layout.activity_main);

        textLastWakeup = findViewById(R.id.text_last_wakeup);
        textNotifyStatus = findViewById(R.id.text_notify_status);
        findViewById(R.id.btn_notify_settings).setOnClickListener(v -> openNotificationSettings());

        requestPostNotificationsIfNeeded();

        monitor = ((UEventDemoApplication) getApplication()).getUEventMonitor();
        monitor.setWakeupCallback(this::onWakeupDetected);
        refreshNotifyStatus();
    }

    private void requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[] { Manifest.permission.POST_NOTIFICATIONS },
                        REQ_POST_NOTIFICATIONS);
            }
        }
    }

    private void onWakeupDetected() {
        if (isDestroyed()) {
            return;
        }
        DateFormat fmt =
                DateFormat.getDateTimeInstance(
                        DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());
        textLastWakeup.setText(fmt.format(new Date()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotifyStatus();
    }

    private void refreshNotifyStatus() {
        boolean granted =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                                == PackageManager.PERMISSION_GRANTED;
        textNotifyStatus.setText(
                granted ? R.string.notif_status_on : R.string.notif_status_off);
    }

    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        try {
            startActivity(intent);
        } catch (Exception e) {
            startActivity(
                    new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null)));
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_POST_NOTIFICATIONS) {
            refreshNotifyStatus();
        }
    }

    @Override
    protected void onDestroy() {
        if (monitor != null) {
            monitor.setWakeupCallback(null);
        }
        super.onDestroy();
    }
}
