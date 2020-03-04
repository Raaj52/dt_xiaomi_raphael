/*
 * Copyright (C) 2019 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.chaldeastudio.deviceparts.fod;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.Log;
import androidx.preference.PreferenceManager;

import vendor.xiaomi.hardware.displayfeature.V1_0.IDisplayFeature;

public class FodService extends Service {

    private static final String TAG = "FodService";
    private static final boolean DEBUG = false;

    private static final String FOD_SCRNOFF_SETTING_KEY = "fod_screenoff_enable";
    private static final String FOD_SCRNOFFD_PROP = "persist.sys.gfscreenoffd.run";

    private static boolean isFodScreenOffEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(FOD_SCRNOFF_SETTING_KEY, false);
    }

    private void listenFodScreenOff(boolean enable) {
        boolean running = SystemProperties.getInt(FOD_SCRNOFFD_PROP, 0) != 0;

        if (enable && running) {
            if (DEBUG) Log.i(TAG, "gfscreenoffd already running");
            return;
        }

        SystemProperties.set(FOD_SCRNOFFD_PROP, enable ? "1" : "0");
    }

    @Override
    public void onCreate() {
        registerReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting service");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service");
        this.unregisterReceiver(mIntentReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        this.registerReceiver(mIntentReceiver, filter);
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                try {
                    IDisplayFeature mDisplayFeature = IDisplayFeature.getService();
                    mDisplayFeature.setFeature(0, 0, 2, 255);
                    mDisplayFeature.setFeature(0, 3, 0, 255);
                } catch(Exception e) {
                }

                if (isFodScreenOffEnabled(context)) {
                    listenFodScreenOff(false);
                }
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                if (isFodScreenOffEnabled(context)) {
                    listenFodScreenOff(true);
                }
            }
        }
    };

}
