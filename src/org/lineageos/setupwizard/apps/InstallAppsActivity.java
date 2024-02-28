/*
 * SPDX-FileCopyrightText: 2019 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.apps;

import static org.lineageos.setupwizard.SetupWizardApp.ACTION_SETUP_INSTALL;

import android.content.Intent;

import org.lineageos.setupwizard.SubBaseActivity;

public class InstallAppsActivity extends SubBaseActivity {

    @Override
    protected void onStartSubactivity() {
        Intent intent = new Intent(ACTION_SETUP_INSTALL);
        startSubactivity(intent);
    }
}
