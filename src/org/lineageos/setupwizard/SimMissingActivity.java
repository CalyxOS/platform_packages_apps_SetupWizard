/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.setupcompat.template.FooterButtonStyleUtils;
import com.google.android.setupcompat.util.WizardManagerHelper;

import org.lineageos.setupwizard.util.SetupWizardUtils;

public class SimMissingActivity extends SubBaseActivity {

    public static final String TAG = SimMissingActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SetupWizardUtils.simMissing(this)) {
            finishAction(RESULT_OK);
        }
    }

    @Override
    protected void onStartSubactivity() {
        setNextAllowed(true);
        getGlifLayout().setDescriptionText(getString(R.string.sim_missing_summary));
        Button manageSimsButton = findViewById(R.id.manage_sims);
        manageSimsButton.setOnClickListener(v -> manageSims());
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.sim_missing_page;
    }

    @Override
    protected int getTitleResId() {
        return R.string.setup_sim_missing;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_sim;
    }

    private void manageSims() {
        Intent intent = new Intent("android.settings.MANAGE_ALL_SIM_PROFILES_SETTINGS");
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Start without the SUW extras that are added by the overridden `startActivity(Intent)`
            // as those break the "SIMs" header.
            startActivity(intent, /* options */ null);
        } else {
            Log.e(TAG, "No activity available to handle " + intent.getAction());
        }
    }

}
