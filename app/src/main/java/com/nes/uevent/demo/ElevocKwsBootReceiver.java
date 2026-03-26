package com.nes.uevent.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 开机完成后设置 elevoc_kws_model（与 {@link UEventDemoApplication} 启动时一致）。
 */
public class ElevocKwsBootReceiver extends BroadcastReceiver {

    private static final String TAG = "ElevocKwsBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        Log.i(TAG, "BOOT_COMPLETED, schedule elevoc_kws_model (delayed)");
        // 开机后音频/ Elevoc 初始化较晚，过早 setParameters 易导致 switchKwsModel 路径错误、nn init 失败
        UEventDemoApplication.scheduleApplyElevocKwsModel(context, 12_000L);
    }
}
