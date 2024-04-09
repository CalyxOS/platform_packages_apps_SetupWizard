/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.service.euicc.EuiccService;
import android.telephony.euicc.EuiccManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.google.android.setupcompat.template.FooterButtonStyleUtils;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.util.ThemeHelper;

import org.lineageos.setupwizard.util.SetupWizardUtils;

public class SimMissingActivity extends SubBaseActivity {

    public static final String TAG = SimMissingActivity.class.getSimpleName();

    // From com.android.settings.network.telephony.MobileNetworkUtils
    // System Property which is used to decide whether the default eSIM UI will be shown,
    // the default value is false.
    private static final String KEY_ENABLE_ESIM_UI_BY_DEFAULT =
            "esim.enable_esim_system_ui_by_default";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SetupWizardUtils.simMissing(this)) {
            finishAction(RESULT_OK);
        }
    }

    protected void startActivityForResult(@NonNull Intent intent) {
        intent.putExtra(WizardManagerHelper.EXTRA_THEME, ThemeHelper.THEME_GLIF_V4);
        activityResultLauncher.launch(intent);
    }

    @Override
    protected void onStartSubactivity() {
        setNextAllowed(true);
        EuiccManager euiccManager = (EuiccManager) getSystemService(Context.EUICC_SERVICE);
        if (euiccManager.isEnabled() /*&& NetworkMonitor.getInstance().isNetworkConnected()*/
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
        if (intent.resolveActivity(getPackageManager()) != null) {
            startSubactivity(intent);
        } else {
            Log.e(TAG, "No activity available to handle " + intent.getAction());
        }
    }

}
