package com.nes.uevent.demo;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 应用入口。进程启动时：设置 support_ok_freebox=1；通过 AudioManager 设置 elevoc_kws_model。
 */
public class UEventDemoApplication extends Application {

    private static final String TAG = "UEventDemoApplication";

    /**
     * 处于「已 onStart 且未 onStop」的 Activity 数量。用 start/stop 比 resume/pause 更接近「界面是否对用户可见」：
     * TV/Leanback 上按 Home 后有时序差异；仅用 pause 可能仍误判为前台，从而走 Toast（后台被系统吃掉 → 看起来像没弹窗）。
     */
    private static final AtomicInteger sStartedActivityCount = new AtomicInteger(0);

    public static boolean isAppInForeground() {
        return sStartedActivityCount.get() > 0;
    }
    /** 需在应用启动时写入的系统属性名 */
    private static final String PROP_SUPPORT_OK_FREEBOX = "support_ok_freebox";
    private static final String PROP_VALUE = "1";

    /**
     * elevoc_kws_model 取值：{@code freebox}（关键词）或 {@code 2}（数字），按平台要求择一。
     */
    private static final String ELEVOC_KWS_MODEL_VALUE = "freebox";

    /**
     * 勿在 Application.onCreate 立刻调用 setParameters：音频栈未就绪时切换 KWS 会导致 HAL 里 model 路径异常
     *（如 /.ckpt-...）、nn init 失败，甚至影响整机语音。延迟到主线程空闲后再设。
     */
    private static final long ELEVOC_KWS_APPLY_DELAY_APP_MS = 4000L;

    /** 进程内常驻监听，避免 MainActivity onDestroy 里 unregister 后后台再也收不到 UEvent。 */
    private UEventMonitor mUEventMonitor;

    @Override
    public void onCreate() {
        super.onCreate();
        WakeFeedbackHelper.ensureNotificationChannel(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(Activity activity) {
                sStartedActivityCount.incrementAndGet();
            }

            @Override
            public void onActivityResumed(Activity activity) {}

            @Override
            public void onActivityPaused(Activity activity) {}

            @Override
            public void onActivityStopped(Activity activity) {
                int n = sStartedActivityCount.decrementAndGet();
                if (n < 0) {
                    sStartedActivityCount.set(0);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        });
        mUEventMonitor = new UEventMonitor(this);
        mUEventMonitor.registerAudioUEvent();
        Log.i(TAG, "UEventMonitor registered (application scope)");
        startKeepAliveService();
        setSupportOkFreebox();
        scheduleApplyElevocKwsModel(this, ELEVOC_KWS_APPLY_DELAY_APP_MS);
    }

    public UEventMonitor getUEventMonitor() {
        return mUEventMonitor;
    }

    private void startKeepAliveService() {
        try {
            Intent i = new Intent(this, WakeMonitorService.class);
            ContextCompat.startForegroundService(this, i);
            Log.i(TAG, "WakeMonitorService started");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start WakeMonitorService", t);
        }
    }

    /**
     * 在主线程延迟执行 {@link #applyElevocKwsModelParameters(Context)}，避免与 Activity 首帧、音频初始化抢时机。
     */
    public static void scheduleApplyElevocKwsModel(Context anyContext, long delayMs) {
        if (anyContext == null) {
            return;
        }
        final Context app = anyContext.getApplicationContext();
        Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(
                () -> {
                    try {
                        applyElevocKwsModelParameters(app);
                    } catch (Throwable t) {
                        Log.e("UEventDemoApplication", "Deferred elevoc_kws_model failed", t);
                    }
                },
                Math.max(0L, delayMs));
    }

    /**
     * 设置 {@code elevoc_kws_model}（AudioManager.setParameters）。应用启动与开机广播共用。
     */
    public static void applyElevocKwsModelParameters(Context context) {
        if (context == null) {
            return;
        }
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) {
                Log.e(TAG, "AudioManager is null, skip elevoc_kws_model");
                return;
            }
            String param = "elevoc_kws_model=" + ELEVOC_KWS_MODEL_VALUE;
            am.setParameters(param);
            Log.i(TAG, "setParameters: " + param);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set elevoc_kws_model", e);
        }
    }

    /**
     * 设置 support_ok_freebox=1。
     * 通过反射调用 android.os.SystemProperties.set（系统隐藏 API）。
     * 需系统签名或 system uid 才可能生效。
     */
    private void setSupportOkFreebox() {
        try {
            Class<?> clz = Class.forName("android.os.SystemProperties");
            Method set = clz.getMethod("set", String.class, String.class);
            set.invoke(null, PROP_SUPPORT_OK_FREEBOX, PROP_VALUE);
            Log.i(TAG, "set " + PROP_SUPPORT_OK_FREEBOX + "=" + PROP_VALUE + " ok");
        } catch (Exception e) {
            Log.w(
                    TAG,
                    "Cannot set " + PROP_SUPPORT_OK_FREEBOX + " (need privileged/system write); ignore",
                    e);
        }
    }
}
