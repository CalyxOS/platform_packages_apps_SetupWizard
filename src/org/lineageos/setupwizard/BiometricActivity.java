/*
 * Copyright (C) 2016 The CyanogenMod Project
 *               2017-2022 The LineageOS Project
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

package org.lineageos.setupwizard;

import static org.lineageos.setupwizard.SetupWizardApp.ACTION_SETUP_BIOMETRIC;
import static org.lineageos.setupwizard.SetupWizardApp.REQUEST_CODE_SETUP_BIOMETRIC;

import android.content.Intent;

import org.lineageos.setupwizard.util.GarlicLevelHelper;

public class BiometricActivity extends WrapperSubBaseActivity {

    public static final String TAG = BiometricActivity.class.getSimpleName();

    private GarlicLevelHelper mGarlicLevelHelper;

    @Override
    protected void onStartSubactivity() {
        mGarlicLevelHelper = new GarlicLevelHelper(this);
        Intent intent = new Intent(ACTION_SETUP_BIOMETRIC);
        intent = mGarlicLevelHelper.putMinPasswordComplexityToIntent(intent);
        startSubactivity(intent, REQUEST_CODE_SETUP_BIOMETRIC);
    }

    @Override
    protected void onSubactivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SETUP_BIOMETRIC) {
            if (resultCode != RESULT_OK) {
                final boolean insufficientPassword =
                        mGarlicLevelHelper.maybeShowInsufficientPasswordDialog(dialog -> {
                            super.onSubactivityResult(requestCode, RESULT_CANCELED, null);
                        });
                if (insufficientPassword) {
                    return;
                }
            }
        }
        super.onSubactivityResult(requestCode, resultCode, data);
    }
}
