/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017-2020 The LineageOS Project
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.service.euicc.EuiccService;
import android.telephony.euicc.EuiccManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.android.setupcompat.util.ResultCodes;

import org.calyxos.setupwizard.util.PhoneMonitor;
import org.calyxos.setupwizard.util.SetupWizardUtils;

import static android.service.euicc.EuiccService.ACTION_PROVISION_EMBEDDED_SUBSCRIPTION;
import static android.telephony.euicc.EuiccManager.EXTRA_FORCE_PROVISION;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_FIRST_RUN;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW;
import static org.calyxos.setupwizard.SetupWizardApp.REQUEST_CODE_SETUP_EUICC;

public class SimMissingActivity extends SubBaseActivity {

    public static final String TAG = SimMissingActivity.class.getSimpleName();

    // From com.android.settings.network.telephony.MobileNetworkUtils
    // System Property which is used to decide whether the default eSIM UI will be shown,
    // the default value is false.
    private static final String KEY_ENABLE_ESIM_UI_BY_DEFAULT =
            "esim.enable_esim_system_ui_by_default";

    private static final int SIM_DEFAULT = 0;
    private static final int SIM_SIDE = 1;
    private static final int SIM_BACK = 2;

    private PhoneMonitor mPhoneMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPhoneMonitor = PhoneMonitor.getInstance();
        if (!mPhoneMonitor.simMissing()) {
            finishAction(RESULT_OK);
        }
        setNextText(R.string.skip);
        final int simLocation = getResources().getInteger(
                R.integer.sim_image_type);
        ImageView simLogo = ((ImageView) findViewById(R.id.sim_slot_image));
        switch (simLocation) {
            case SIM_SIDE:
                simLogo.setImageResource(R.drawable.sim_side);
                break;
            case SIM_BACK:
                simLogo.setImageResource(R.drawable.sim_back);
                break;
            default:
                simLogo.setImageResource(R.drawable.sim);
                simLogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
    }

    @Override
    protected void onStartSubactivity() {
        setNextAllowed(true);
        final boolean enabledEsimUiByDefault =
                SystemProperties.getBoolean(KEY_ENABLE_ESIM_UI_BY_DEFAULT, true);
        EuiccManager euiccManager = (EuiccManager) getSystemService(Context.EUICC_SERVICE);
        if (euiccManager.isEnabled() && enabledEsimUiByDefault) {
            findViewById(R.id.setup_euicc).setOnClickListener(v -> launchEuiccSetup());
        } else {
            findViewById(R.id.euicc).setVisibility(View.GONE);
            findViewById(R.id.setup_euicc).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SetupWizardUtils.enableComponent(this, ChooseDataSimActivity.class);
        SetupWizardUtils.enableComponent(this, MobileDataActivity.class);
    }

    @Override
    public void onNavigateNext() {
        if (mPhoneMonitor.simMissing()) {
            SetupWizardUtils.disableComponent(this, ChooseDataSimActivity.class);
            SetupWizardUtils.disableComponent(this, MobileDataActivity.class);
            nextAction(ResultCodes.RESULT_SKIP);
        } else {
            super.onNavigateNext();
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
        Intent intent = new Intent(ACTION_PROVISION_EMBEDDED_SUBSCRIPTION);
        intent.putExtra(EXTRA_FORCE_PROVISION, true);
        intent.putExtra(EXTRA_IS_FIRST_RUN, true);
        intent.putExtra(EXTRA_IS_SETUP_FLOW, true);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startSubactivity(intent, REQUEST_CODE_SETUP_EUICC);
        } else {
            Log.e(TAG, "No activity available to handle " + intent.getAction());
        }
    }

}