/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
 * Copyright (c) 2020 Chaldeastudio Project
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

package org.chaldeastudio.deviceparts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

import com.android.internal.os.DeviceKeyHandler;
import com.android.internal.util.ArrayUtils;

public class KeyHandler implements DeviceKeyHandler {
    private static final String TAG = "KeyHandler";
    private static final boolean DEBUG = true;

    private static final int KEY_FOD_GESTURE_DOWN = 745;
    private static final String DOZE_INTENT = "com.android.systemui.doze.pulse";
    private static final String FOD_SCRNOFFD_PROP = "persist.sys.gfscreenoffd.run";
    private static final String FOD_SCRNOFF_SETTING_KEY = "fod_screenoff_enable";

    private static final int[] sSupportedGestures = new int[]{
        KEY_FOD_GESTURE_DOWN
    };

    private boolean mInteractive = true;

    private Handler mHandler;
    private final PowerManager mPowerManager;
    protected final Context mContext;

    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                 mInteractive = true;
                if (isFodScreenOffEnabled()) {
                    listenFodScreenOff(false);
                }
             } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                 mInteractive = false;
                if (isFodScreenOffEnabled()) {
                    listenFodScreenOff(true);
                }
             }
         }
    };

    public KeyHandler(Context context) {
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(mScreenStateReceiver, screenStateFilter);
    }

    private boolean hasSetupCompleted() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.USER_SETUP_COMPLETE, 0) != 0;
    }

    private boolean isFodScreenOffEnabled() {
        try {
            return Settings.System.getIntForUser(mContext.getContentResolver(),
                FOD_SCRNOFF_SETTING_KEY, UserHandle.USER_CURRENT) != 0;
        } catch(Exception e) {
            return false;
        }
    }

    private void listenFodScreenOff(boolean enable) {
        boolean running = SystemProperties.getInt(FOD_SCRNOFFD_PROP, 0) != 0;

        if (enable && running) {
            if (DEBUG) Log.i(TAG, "gfscreenoffd already running");
            return;
        }

        SystemProperties.set(FOD_SCRNOFFD_PROP, enable ? "1" : "0");
    }

    private void launchDozePulse() {
        if (DEBUG) Log.i(TAG, "Doze pulse");
        mContext.sendBroadcastAsUser(new Intent(DOZE_INTENT),
                new UserHandle(UserHandle.USER_CURRENT));
    }

    private void handleFODScreenOff(int eventCode) {
        if (!isFodScreenOffEnabled() && mInteractive) {
            return;
        }

        if (eventCode != KEY_FOD_GESTURE_DOWN) {
            return;
        }

        launchDozePulse();
    }

    @Override
    public boolean handleKeyEvent(KeyEvent event) {
        if (!hasSetupCompleted()) {
            return false;
        }

        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }
        
        handleFODScreenOff(event.getScanCode());

        return ArrayUtils.contains(sSupportedGestures, event.getScanCode());
    }

    @Override
    public boolean canHandleKeyEvent(KeyEvent event) {
        if (!hasSetupCompleted()) {
            return false;
        }

        return ArrayUtils.contains(sSupportedGestures, event.getScanCode());
    }

    @Override
    public boolean isDisabledKeyEvent(KeyEvent event) {
        return false;
    }

    @Override
    public boolean isCameraLaunchEvent(KeyEvent event) {
        return false;
    }

    @Override
    public boolean isWakeEvent(KeyEvent event){
        return false;
    }

    @Override
    public Intent isActivityLaunchEvent(KeyEvent event) {
        return null;
    }

}
