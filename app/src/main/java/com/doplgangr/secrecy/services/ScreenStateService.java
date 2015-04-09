package com.doplgangr.secrecy.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.doplgangr.secrecy.events.ScreenOffEvent;

import de.greenrobot.event.EventBus;

/**
 * This service registers a {@code android.content.BroadcastReceiver} to catch {@code android.intent.action.SCREEN_OFF}.
 */
public class ScreenStateService extends Service {
    private static final String SCREEN_OFF = "android.intent.action.SCREEN_OFF";
    private static final String SCREEN_ON = "android.intent.action.SCREEN_ON";
    private ScreenStateReceiver mScreenStateReceiver;

    public ScreenStateService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mScreenStateReceiver = new ScreenStateReceiver();
        final IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenStateFilter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mScreenStateReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    class ScreenStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(SCREEN_OFF)) {
                EventBus.getDefault().post(new ScreenOffEvent());
            }
        }
    }
}