/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017-2018,2020 The LineageOS Project
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

package org.calyxos.setupwizard;

import android.content.ComponentName;
import android.content.Intent;

import static org.calyxos.setupwizard.SetupWizardApp.REQUEST_CODE_SETUP_EUICC;

public class EuiccSetupActivity extends WrapperSubBaseActivity {

    public static final String TAG = EuiccSetupActivity.class.getSimpleName();

    @Override
    protected void onStartSubactivity() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.google.android.euicc", "com.android.euicc.ui.suw.CurrentSuwInitActivity"));
        startSubactivity(intent, REQUEST_CODE_SETUP_EUICC);
    }
}
