/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static org.lineageos.setupwizard.SetupWizardApp.EXTRA_ENABLE_NEXT_ON_CONNECT;
import static org.lineageos.setupwizard.SetupWizardApp.EXTRA_PREFS_SET_BACK_TEXT;
import static org.lineageos.setupwizard.SetupWizardApp.EXTRA_PREFS_SHOW_BUTTON_BAR;
import static org.lineageos.setupwizard.SetupWizardApp.EXTRA_PREFS_SHOW_SKIP;
import static org.lineageos.setupwizard.SetupWizardApp.EXTRA_PREFS_SHOW_SKIP_TV;

import android.content.Intent;
import android.os.Bundle;

import org.lineageos.setupwizard.util.SetupWizardUtils;

public class NetworkSetupActivity extends SubBaseActivity {

    private static String ACTION_SETUP_NETWORK = "android.settings.NETWORK_PROVIDER_SETUP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SetupWizardUtils.hasLeanback(this)) {
            ACTION_SETUP_NETWORK = "com.android.net.wifi.SETUP_WIFI_NETWORK";
        }
    }

    @Override
    protected void onStartSubactivity() {
        Intent intent = new Intent(ACTION_SETUP_NETWORK);
        intent.putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, true);
        intent.putExtra(EXTRA_PREFS_SHOW_SKIP, true);
        intent.putExtra(EXTRA_PREFS_SHOW_SKIP_TV, true);
        intent.putExtra(EXTRA_PREFS_SET_BACK_TEXT, (String) null);
        intent.putExtra(EXTRA_ENABLE_NEXT_ON_CONNECT, true);
        startSubactivity(intent);
    }
}
