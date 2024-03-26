/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static org.lineageos.setupwizard.SetupWizardApp.ACTION_SETUP_NETWORK;
import static org.lineageos.setupwizard.SetupWizardApp.EXTRA_ENABLE_NEXT_ON_CONNECT;
import static org.lineageos.setupwizard.SetupWizardApp.EXTRA_PREFS_SET_BACK_TEXT;
import static org.lineageos.setupwizard.SetupWizardApp.EXTRA_PREFS_SHOW_BUTTON_BAR;
import static org.lineageos.setupwizard.SetupWizardApp.EXTRA_PREFS_SHOW_SKIP;
import static org.lineageos.setupwizard.SetupWizardApp.EXTRA_PREFS_SHOW_SKIP_TV;

import android.content.Intent;

import org.lineageos.setupwizard.util.SetupWizardUtils;

public class NetworkSetupActivity extends SubBaseActivity {

    @Override
    protected void onStartSubactivity() {
        Intent intent = new Intent(ACTION_SETUP_NETWORK);
        if (SetupWizardUtils.hasLeanback(this)) {
            intent.setComponent(SetupWizardUtils.sTvWifiSetupSettingsActivity);
        }
        intent.putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, true);
        intent.putExtra(EXTRA_PREFS_SHOW_SKIP, true);
        intent.putExtra(EXTRA_PREFS_SHOW_SKIP_TV, true);
        intent.putExtra(EXTRA_PREFS_SET_BACK_TEXT, (String) null);
        intent.putExtra(EXTRA_ENABLE_NEXT_ON_CONNECT, true);
        startSubactivity(intent);
    }
}
