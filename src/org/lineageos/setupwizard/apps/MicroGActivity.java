/*
 * SPDX-FileCopyrightText: 2019-2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.apps;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

import android.annotation.Nullable;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.lineageos.setupwizard.BaseSetupWizardActivity;
import org.lineageos.setupwizard.R;
import org.lineageos.setupwizard.util.SetupWizardUtils;

public class MicroGActivity extends BaseSetupWizardActivity {

    private static final String[] MICROG_PACKAGES = new String[]{
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.vending"
    };

    private PackageManager pm;
    private Switch enableSwitch;
    private Switch enablePush;
    private Switch enableLocation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setNextText(R.string.next);
        getGlifLayout().setDescriptionText(getString(R.string.microg_description2));

        enableSwitch = findViewById(R.id.enableSwitch);
        enablePush = findViewById(R.id.enablePush);
        enableLocation = findViewById(R.id.enableLocation);
        enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enablePush.setEnabled(isChecked);
            enablePush.setChecked(isChecked);
            enableLocation.setEnabled(isChecked);
            enableLocation.setChecked(isChecked);
        });

        final TextView enableSummary = findViewById(R.id.enableSummary);
        final String profileType = SetupWizardUtils.isManagedProfile(this)
                ? getString(R.string.managed_profile)
                : getString(R.string.personal_profile);
        enableSummary.setText(getString(R.string.microg_summary, profileType));

        pm = getPackageManager();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.microg_activity;
    }

    @Override
    protected int getTitleResId() {
        return R.string.microg_title;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.microg_logo;
    }

    @Override
    public void onNextPressed() {
        for (String packageId : MICROG_PACKAGES) {
            setAppEnabled(packageId, enableSwitch.isChecked());
        }
        if (enableSwitch.isChecked()) {
            Intent intent = new Intent();
            intent.setClassName("com.google.android.gms",
                    "org.microg.gms.provision.ProvisionService");
            intent.putExtra("checkin_enabled", enablePush.isChecked());
            intent.putExtra("gcm_enabled", enablePush.isChecked());
            intent.putExtra("wifi_mls", enableLocation.isChecked());
            intent.putExtra("cell_mls", enableLocation.isChecked());
            intent.putExtra("ichnaea_endpoint", R.string.microg_ichnaea_endpoint);
            intent.putExtra("wifi_learning", enableLocation.isChecked());
            intent.putExtra("cell_learning", enableLocation.isChecked());
            intent.putExtra("nominatim_enabled", enableLocation.isChecked());
            intent.putExtra("safetynet_enabled", false);
            startService(intent);
        }
        super.onNextPressed();
    }

    private void setAppEnabled(String packageName, boolean enabled) {
        int state = enabled ? COMPONENT_ENABLED_STATE_ENABLED : COMPONENT_ENABLED_STATE_DISABLED;
        pm.setApplicationEnabledSetting(packageName, state, 0);
    }
}
