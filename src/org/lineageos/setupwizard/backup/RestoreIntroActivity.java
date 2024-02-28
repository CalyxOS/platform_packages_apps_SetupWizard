/*
 * SPDX-FileCopyrightText: 2019-2020 The Calyx Institute
 * SPDX-FileCopyrightText: 2020-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.backup;

import static org.lineageos.setupwizard.SetupWizardApp.ACTION_RESTORE_FROM_BACKUP;

import android.content.Intent;
import android.os.Bundle;

import org.lineageos.setupwizard.R;
import org.lineageos.setupwizard.SubBaseActivity;

public class RestoreIntroActivity extends SubBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getGlifLayout().setDescriptionText(getString(R.string.intro_restore_subtitle,
                getString(R.string.os_name)));
    }

    @Override
    protected void onStartSubactivity() {
        setNextAllowed(true);
    }

    @Override
    protected void onNextPressed() {
        launchRestore();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.intro_restore_activity;
    }

    @Override
    protected int getTitleResId() {
        return R.string.intro_restore_title;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_restore;
    }

    private void launchRestore() {
        Intent intent = new Intent(ACTION_RESTORE_FROM_BACKUP);
        startSubactivity(intent);
    }

}
