package com.nes.uevent.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * Same visible wake-up card for foreground and background. {@code singleTop}: repeat wake-ups
 * while visible reset the auto-close timer instead of being dropped by the system.
 */
public class WakeupPopupActivity extends Activity {

    private static final long AUTO_FINISH_MS = 1500L;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        setFinishOnTouchOutside(true);
        setContentView(R.layout.activity_wakeup_popup);
        findViewById(android.R.id.content).setOnClickListener(v -> finish());
        scheduleAutoFinish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        scheduleAutoFinish();
    }

    private void scheduleAutoFinish() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(this::finish, AUTO_FINISH_MS);
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
