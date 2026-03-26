package com.nes.uevent.demo;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

public class MainActivity extends Activity {

    UEventMonitor monitor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        monitor = new UEventMonitor(this);
        monitor.registerAudioUEvent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        monitor.unRegisterAudioEvent();
    }
}
