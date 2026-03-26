package com.nes.uevent.demo;

import android.app.Application;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * 应用入口。在进程启动时设置 support_ok_freebox=1（系统属性）。
 */
public class UEventDemoApplication extends Application {

    private static final String TAG = "UEventDemoApplication";
    /** 需在应用启动时写入的系统属性名 */
    private static final String PROP_SUPPORT_OK_FREEBOX = "support_ok_freebox";
    private static final String PROP_VALUE = "1";

    @Override
    public void onCreate() {
        super.onCreate();
        setSupportOkFreebox();
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
            Log.e(TAG, "Failed to set " + PROP_SUPPORT_OK_FREEBOX, e);
        }
    }
}
