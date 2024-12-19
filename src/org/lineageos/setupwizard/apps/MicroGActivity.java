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
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.lineageos.setupwizard.BaseSetupWizardActivity;
import org.lineageos.setupwizard.R;
import org.lineageos.setupwizard.util.SetupWizardUtils;

public class MicroGActivity extends BaseSetupWizardActivity {

    private static final String ONLINE_SOURCE_UNSET = "<UNSET>";

    private static final String[] MICROG_PACKAGES = new String[]{
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.vending"
    };

    private PackageManager pm;
    private Switch enableSwitch;
    private Switch enablePush;
    private Switch enableLocation;
    private ViewGroup onlineSourcesList;
    private View onlineSourceRequiredError;
    private String selectedOnlineSource = ONLINE_SOURCE_UNSET;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setNextText(R.string.next);
        getGlifLayout().setDescriptionText(getString(R.string.microg_description2));

        enableSwitch = findViewById(R.id.enableSwitch);
        enablePush = findViewById(R.id.enablePush);
        enableLocation = findViewById(R.id.enableLocation);
        onlineSourcesList = findViewById(R.id.onlineSourcesList);
        onlineSourceRequiredError = findViewById(R.id.onlineSourceRequiredError);
        enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enablePush.setEnabled(isChecked);
            enablePush.setChecked(isChecked);
            enableLocation.setEnabled(isChecked);
            enableLocation.setChecked(isChecked);
        });
        prepareOnlineSourcesList();

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
        if (enableLocation.isChecked() && ONLINE_SOURCE_UNSET.equals(selectedOnlineSource)) {
            onlineSourceRequiredError.setVisibility(View.VISIBLE);
            return;
        }
        for (String packageId : MICROG_PACKAGES) {
            setAppEnabled(packageId, enableSwitch.isChecked());
        }
        if (enableSwitch.isChecked()) {
            Intent intent = new Intent();
            intent.setClassName("com.google.android.gms",
                    "org.microg.gms.provision.ProvisionService");
            intent.putExtra("checkin_enabled", enablePush.isChecked());
            intent.putExtra("gcm_enabled", enablePush.isChecked());
            intent.putExtra("wifi_ichnaea", enableLocation.isChecked());
            intent.putExtra("cell_ichnaea", enableLocation.isChecked());
            intent.putExtra("wifi_learning", enableLocation.isChecked());
            intent.putExtra("cell_learning", enableLocation.isChecked());
            intent.putExtra("nominatim_enabled", enableLocation.isChecked());
            if (!ONLINE_SOURCE_UNSET.equals(selectedOnlineSource)) {
                intent.putExtra("online_source_id", selectedOnlineSource);
                android.util.Log.i("debuggy", "online_source_id: " + selectedOnlineSource);
            }
            intent.putExtra("cell_learning", enableLocation.isChecked());
            startService(intent);
        }
        super.onNextPressed();
    }

    private void setAppEnabled(String packageName, boolean enabled) {
        int state = enabled ? COMPONENT_ENABLED_STATE_ENABLED : COMPONENT_ENABLED_STATE_DISABLED;
        pm.setApplicationEnabledSetting(packageName, state, 0);
    }

    private void prepareOnlineSourcesList() {
        for (int i = 0; i < onlineSourcesList.getChildCount(); i++) {
            final View childView = onlineSourcesList.getChildAt(i);
            if (!childView.isClickable() || !childView.isFocusable()) {
                continue;
            }
            childView.setOnClickListener(view -> {
                // The online source ID is specified as a tag in the microg_activity layout
                // via strings in calyx_strings.xml.
                selectedOnlineSource = (String) view.getTag(R.id.online_source_id);
                onlineSourceRequiredError.setVisibility(View.INVISIBLE);
                highlightView(view);
            });
        }
        enableLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            onlineSourcesList.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }

    private void highlightView(final View view) {
        for (int i = 0; i < onlineSourcesList.getChildCount(); i++) {
            final View childView = onlineSourcesList.getChildAt(i);
            if (!childView.isClickable() || !childView.isFocusable()) {
                continue;
            }
            if (childView.equals(view)) {
                view.setBackground(
                    ContextCompat.getDrawable(this, R.drawable.rectangle_background_focused)
                );
            } else {
                childView.setBackground(
                    ContextCompat.getDrawable(this, R.drawable.rectangle_background)
                );
            }
        }
    }
}
