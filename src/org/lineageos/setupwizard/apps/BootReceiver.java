/*
 * Copyright (C) 2022 The Android Open Source Project
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

package org.lineageos.setupwizard.apps;

import static org.lineageos.setupwizard.SetupWizardApp.PACKAGENAMES;
import static org.lineageos.setupwizard.apps.AppInstallerService.APKS;
import static org.lineageos.setupwizard.apps.AppInstallerService.PATH;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.android.setupcompat.util.WizardManagerHelper;

import org.lineageos.setupwizard.R;

import java.util.ArrayList;
import java.util.HashMap;

public class BootReceiver extends BroadcastReceiver {

    private static final HashMap<String, String> DEMOTED_SYSTEM_APPS =
            new HashMap<>() {{
                put("org.fdroid.fdroid", "F-Droid.apk");
            }};

    @Override
    public void onReceive(Context context, Intent intent) {
        if (WizardManagerHelper.isUserSetupComplete(context)) {
            ArrayList<String> apks = new ArrayList<>();
            ArrayList<String> packageNames = new ArrayList<>();
            for (String key : DEMOTED_SYSTEM_APPS.keySet()) {
                try {
                    context.getPackageManager().getPackageInfo(key, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    apks.add(DEMOTED_SYSTEM_APPS.get(key));
                    packageNames.add(key);
                }
            }
            if (apks.size() > 0) {
                Intent i = new Intent(context, AppInstallerService.class);
                i.putExtra(PATH, context.getString(R.string.calyx_fdroid_repo_location));
                i.putStringArrayListExtra(APKS, apks);
                i.putStringArrayListExtra(PACKAGENAMES, packageNames);
                context.startForegroundService(i);
            }
        }
        context.getPackageManager().setComponentEnabledSetting(
                new ComponentName(context, BootReceiver.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
