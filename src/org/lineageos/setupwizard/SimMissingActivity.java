/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static com.google.android.setupcompat.util.ResultCodes.RESULT_SKIP;

import android.annotation.Nullable;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.service.euicc.EuiccService;
import android.telephony.euicc.EuiccManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;

import com.google.android.setupcompat.template.FooterButtonStyleUtils;

import org.lineageos.setupwizard.util.SetupWizardUtils;

public class SimMissingActivity extends BaseSetupWizardActivity {

    public static final String TAG = SimMissingActivity.class.getSimpleName();

    // From com.android.settings.network.telephony.MobileNetworkUtils
    // System Property which is used to decide whether the default eSIM UI will be shown,
    // the default value is false.
    private static final String KEY_ENABLE_ESIM_UI_BY_DEFAULT =
            "esim.enable_esim_system_ui_by_default";

    private final ActivityResultLauncher<Intent> mEuiccSetupResultLauncher =
            registerForActivityResult(
                    new StartDecoratedActivityForResult(),
                    this::onEuiccSetupActivityResult);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mUseSuwIntentExtras = false;
        super.onCreate(savedInstanceState);
        if (!isAvailable()) {
            // NetworkSetupActivity comes before us. DateTimeActivity comes after.
            // If the user presses the back button on DateTimeActivity, we can only pass along
            // that information to NetworkSetupActivity if we are still around. But if we finish
            // here, we're gone, and NetworkSetupActivity will get whatever result we give here.
            // We can't predict the future, but we can reasonably assume that the only way for
            // NetworkSetupActivity to be reached later is if the user went backwards. So, we
            // finish this activity faking that the user pressed the back button, which is required
            // for subactivities like NetworkSetupActivity to work properly on backward navigation.
            // See also onEuiccSetupActivityResult.
            // TODO: Resolve all this.
            finishAction(RESULT_SKIP, new Intent().putExtra("onBackPressed", true));
            return;
        }
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

    // TODO: Something like this could be in the base class and eliminate a lot of boilerplate.
    private boolean isAvailable() {
        return SetupWizardUtils.simMissing(this) && SetupWizardUtils.hasTelephony(this);
    }

    @Override
    protected void onNextIntentResult(ActivityResult activityResult) {
        if (!isAvailable()) {
            final int resultCode = activityResult.getResultCode();
            final Intent data = activityResult.getData();
            if (resultCode == RESULT_CANCELED
                    && data != null && data.getBooleanExtra("onBackPressed", false)) {
                finishAction(resultCode, data);
            }
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
            mEuiccSetupResultLauncher.launch(intent);
        } else {
            Log.e(TAG, "No activity available to handle " + intent.getAction());
        }
    }

    private void onEuiccSetupActivityResult(ActivityResult activityResult) {
        // We don't really care about the result, but if a SIM is no longer missing, we're done.
        if (!SetupWizardUtils.simMissing(this)) {
            // See comments in onCreate for an explanation.
            finishAction(RESULT_SKIP, new Intent().putExtra("onBackPressed", true));
        }
    }
}
