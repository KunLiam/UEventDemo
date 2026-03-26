package com.nes.uevent.demo;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.os.UEventObserver;

public class UEventMonitor {
    private static final String TAG = "UEventMonitor";
    public static final String PATH_OLD_AUDIOFORMAT_UEVENT = "/devices/platform/auge_sound";
    public static final String PATH_NEW_AUDIOFORMAT_UEVENT = "/devices/platform/auge_sound/sound/card0/controlC0";

    public static final String AUDIO_JIO_ASSIST_WAKEUP = "31";

    public static String PATH_AUDIOFORMAT_UEVENT = PATH_OLD_AUDIOFORMAT_UEVENT;

    private Context mContext;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    public interface WakeupCallback {
        void onWakeup();
    }

    private WakeupCallback mWakeupCallback;

    public void setWakeupCallback(WakeupCallback callback) {
        mWakeupCallback = callback;
    }

    public UEventMonitor(Context context) {
        mContext = context;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            PATH_AUDIOFORMAT_UEVENT = PATH_NEW_AUDIOFORMAT_UEVENT;
        }
    }

    public void registerAudioUEvent() {
        mAudioStatusObserver.startObserving("DEVPATH=" + PATH_AUDIOFORMAT_UEVENT);
    }

    public void unRegisterAudioEvent() {
        mAudioStatusObserver.stopObserving();
    }


    private UEventObserver mAudioStatusObserver = new UEventObserver() {
        public void onUEvent(UEvent event) {
            String audio_format = event.get("AUDIO_FORMAT");
            String devPath = event.get("DEVPATH");
            if (PATH_AUDIOFORMAT_UEVENT.equals(devPath)) {
                Log.i(TAG, "onUEvent: receive audio_format = " + audio_format);
                if (TextUtils.isEmpty(audio_format)) {
                    return;
                }
                if (audio_format.equals(AUDIO_JIO_ASSIST_WAKEUP)) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(
                                    TAG,
                                    "Wakeup UEvent; appUiVisible="
                                            + UEventDemoApplication.isAppInForeground());
                            WakeFeedbackHelper.showWakeupFeedback(mContext);
                            if (mWakeupCallback != null) {
                                mWakeupCallback.onWakeup();
                            }
                        }
                    });
                }
            }
        }
    };

}
