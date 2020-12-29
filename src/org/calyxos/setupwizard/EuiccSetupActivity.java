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

import android.content.Intent;

import static android.service.euicc.EuiccService.ACTION_PROVISION_EMBEDDED_SUBSCRIPTION;
import static android.telephony.euicc.EuiccManager.EXTRA_FORCE_PROVISION;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_FIRST_RUN;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW;
import static org.calyxos.setupwizard.SetupWizardApp.REQUEST_CODE_SETUP_EUICC;

public class EuiccSetupActivity extends WrapperSubBaseActivity {

    public static final String TAG = EuiccSetupActivity.class.getSimpleName();

    @Override
    protected void onStartSubactivity() {
        Intent intent = new Intent(ACTION_PROVISION_EMBEDDED_SUBSCRIPTION);
        intent.putExtra(EXTRA_FORCE_PROVISION, true);
        intent.putExtra(EXTRA_IS_FIRST_RUN, true);
        intent.putExtra(EXTRA_IS_SETUP_FLOW, true);
        startSubactivity(intent, REQUEST_CODE_SETUP_EUICC);
    }
}