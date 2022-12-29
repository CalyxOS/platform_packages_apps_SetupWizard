/*
 * Copyright (C) 2019 The Calyx Institute
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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

    public static final String TAG = MicroGActivity.class.getSimpleName();
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

        final TextView enableDescription = findViewById(R.id.enableDescription);
        final String profileType = SetupWizardUtils.isManagedProfile(this)
                ? getString(R.string.managed_profile)
                : getString(R.string.personal_profile);
        enableDescription.setText(getString(R.string.microg_description, profileType));

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
