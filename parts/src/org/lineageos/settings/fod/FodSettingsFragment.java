/*
 * Copyright (C) 2019 The LineageOS Project
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

package org.lineageos.settings.fod;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.MenuItem;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import org.lineageos.settings.R;

public class FodSettingsFragment extends PreferenceFragment implements
        OnPreferenceChangeListener {

    private SwitchPreference mFodScreenOffPreference;
    private static final String FOD_SCREENOFF_ENABLE_KEY = "fod_screenoff_enable";
    private static final String FOD_SCRNOFFD_PROP = "persist.sys.gfscreenoffd.run";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.fod_settings);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

        mFodScreenOffPreference = (SwitchPreference) findPreference(FOD_SCREENOFF_ENABLE_KEY);
        mFodScreenOffPreference.setEnabled(true);
        mFodScreenOffPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (FOD_SCREENOFF_ENABLE_KEY.equals(preference.getKey())) {
            enableFodScreenOff((Boolean) newValue);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        }
        return false;
    }

    private void enableFodScreenOff(boolean enable) {
        boolean running = SystemProperties.getInt(FOD_SCRNOFFD_PROP, 0) != 0;

        if (enable && running) return;
        SystemProperties.set(FOD_SCRNOFFD_PROP, enable ? "1" : "0");
    }

}
