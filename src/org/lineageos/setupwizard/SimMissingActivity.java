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

import static org.lineageos.setupwizard.SetupWizardApp.REQUEST_CODE_SETUP_EUICC;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.service.euicc.EuiccService;
import android.telephony.euicc.EuiccManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.setupcompat.template.FooterButtonStyleUtils;
import com.google.android.setupcompat.util.ResultCodes;
import com.google.android.setupcompat.util.WizardManagerHelper;

import org.lineageos.setupwizard.util.NetworkMonitor;
import org.lineageos.setupwizard.util.PhoneMonitor;

public class SimMissingActivity extends SubBaseActivity {

    public static final String TAG = SimMissingActivity.class.getSimpleName();

    // From com.android.settings.network.telephony.MobileNetworkUtils
    // System Property which is used to decide whether the default eSIM UI will be shown,
    // the default value is false.
    private static final String KEY_ENABLE_ESIM_UI_BY_DEFAULT =
            "esim.enable_esim_system_ui_by_default";

    private PhoneMonitor mPhoneMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPhoneMonitor = PhoneMonitor.getInstance();
        if (!mPhoneMonitor.simMissing()) {
            finishAction(RESULT_OK);
        }
    }

    @Override
    protected void onStartSubactivity() {
        setNextAllowed(true);
        EuiccManager euiccManager = (EuiccManager) getSystemService(Context.EUICC_SERVICE);
        if (euiccManager.isEnabled() && NetworkMonitor.getInstance().isNetworkConnected()
                && SystemProperties.getBoolean(KEY_ENABLE_ESIM_UI_BY_DEFAULT, true)) {
            getGlifLayout().setDescriptionText(getString(R.string.sim_missing_full_description,
                    getString(R.string.sim_missing_summary),
                    getString(R.string.euicc_summary)));
            Button setupEuiccButton = findViewById(R.id.setup_euicc);
            setupEuiccButton.setOnClickListener(v -> launchEuiccSetup());
            FooterButtonStyleUtils.applyPrimaryButtonPartnerResource(this, setupEuiccButton, true);
        } else {
            getGlifLayout().setDescriptionText(getString(R.string.sim_missing_summary));
            findViewById(R.id.setup_euicc).setVisibility(View.GONE);
        }
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

    private void launchEuiccSetup() {
        Intent intent = new Intent(EuiccService.ACTION_PROVISION_EMBEDDED_SUBSCRIPTION);
        intent.putExtra(EuiccManager.EXTRA_FORCE_PROVISION, true);
        intent.putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true);
        intent.putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startSubactivity(intent, REQUEST_CODE_SETUP_EUICC);
        } else {
            Log.e(TAG, "No activity available to handle " + intent.getAction());
        }
    }

}
