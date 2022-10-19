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

import org.lineageos.setupwizard.BaseSetupWizardActivity;
import org.lineageos.setupwizard.R;

public class MicroGActivity extends BaseSetupWizardActivity {

    public static final String TAG = MicroGActivity.class.getSimpleName();
    private static final String[] MICROG_PACKAGES = new String[]{
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.vending",
            "org.fitchfamily.android.dejavu",
            "org.microg.nlp.backend.ichnaea",
            "org.microg.nlp.backend.nominatim"
    };

    private PackageManager pm;
    private Switch enableSwitch;
    private Switch enableDefaults;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setNextText(R.string.next);
        getGlifLayout().setDescriptionText(getString(R.string.microg_description2));

        enableSwitch = findViewById(R.id.enableSwitch);
        enableDefaults = findViewById(R.id.enableDefaults);
        enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableDefaults.setEnabled(isChecked);
            enableDefaults.setChecked(isChecked);
        });

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
        if (enableDefaults.isEnabled()) {
            Intent intent = new Intent();
            intent.setClassName("com.google.android.gms",
                    "org.microg.gms.provision.ProvisionService");
            intent.putExtra("checkin_enabled", enableDefaults.isChecked());
            intent.putExtra("gcm_enabled", enableDefaults.isChecked());
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
