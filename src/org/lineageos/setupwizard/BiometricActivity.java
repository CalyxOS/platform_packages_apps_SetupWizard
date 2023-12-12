/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.content.Intent;
import android.provider.Settings;

import androidx.activity.result.ActivityResult;

import org.lineageos.setupwizard.util.ManagedProvisioningUtils;

public class BiometricActivity extends SubBaseActivity {

    public static final String TAG = BiometricActivity.class.getSimpleName();

    @Override
    protected void onStartSubactivity() {
        Intent intent = ManagedProvisioningUtils.putMinPasswordComplexityToIntent(this,
                new Intent(Settings.ACTION_BIOMETRIC_ENROLL));
        startSubactivity(intent);
    }

    @Override
    protected void onActivityResult(ActivityResult activityResult) {
        if (activityResult.getResultCode() != RESULT_OK) {
            final boolean insufficientPassword =
                    ManagedProvisioningUtils.maybeShowInsufficientPasswordDialog(this,
                            dialogInterface -> {
                                ActivityResult newActivityResult = new ActivityResult(
                                        RESULT_CANCELED, activityResult.getData());
                                super.onActivityResult(newActivityResult);
                            });
            if (insufficientPassword) {
                return;
            }
        }
        super.onActivityResult(activityResult);
    }
}
