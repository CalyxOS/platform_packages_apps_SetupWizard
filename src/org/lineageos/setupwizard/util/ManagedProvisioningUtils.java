/*
 * Copyright (C) 2022 The Calyx Institute
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

package org.lineageos.setupwizard.util;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.os.Process;

import lineageos.providers.LineageSettings;

public class ManagedProvisioningUtils {

    private static final String BELLIS_PACKAGE = "org.calyxos.bellis";
    private static final String BELLIS_DEVICE_ADMIN_RECEIVER_CLASS = ".BasicDeviceAdminReceiver";

    private enum GarlicLevel {
        STANDARD,
        SAFER,
        SAFEST
    }

    public static void init(Context context) {
        startProvisioning(context);
    }

    private static void startProvisioning(Context context) {
        // Refresh the state of USER_SETUP_COMPLETE to ease testing
        // TODO: REMOVE FOR PRODUCTION
        DevicePolicyManager devicePolicyManager = context.getSystemService(
                DevicePolicyManager.class);
        devicePolicyManager.forceUpdateUserSetupComplete(Process.myUserHandle().getIdentifier());

        PersistableBundle persistableBundle = new PersistableBundle();
        int provisioningMode = getProvisioningMode(context);
        persistableBundle.putInt(DevicePolicyManager.EXTRA_PROVISIONING_MODE, provisioningMode);
        persistableBundle.putInt(LineageSettings.Global.GARLIC_LEVEL,
                LineageSettings.Global.getInt(context.getContentResolver(),
                        LineageSettings.Global.GARLIC_LEVEL, 0));

        Intent intent = new Intent(
                DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE)
                .putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                        new ComponentName(BELLIS_PACKAGE, BELLIS_PACKAGE
                                + BELLIS_DEVICE_ADMIN_RECEIVER_CLASS))
                .putExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
                        persistableBundle)
                .putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS, true);
        if (provisioningMode == DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE) {
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED,
                    true);
        }
        context.startActivityForResult("", intent, 0, null);
    }

    private static int getProvisioningMode(Context context) {
        return LineageSettings.Global.getInt(context.getContentResolver(),
                LineageSettings.Global.GARLIC_LEVEL, 0) == GarlicLevel.STANDARD.ordinal()
                ? GarlicLevel.STANDARD.ordinal()
                : DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE;
    }

    public static boolean isProvisioningAllowed(Context context) {
        // Only system user can provision the device
        if (SetupWizardUtils.isOwner()) {
            int provisioningMode = getProvisioningMode(context);
            if (provisioningMode != GarlicLevel.STANDARD.ordinal()) {
                DevicePolicyManager devicePolicyManager = context.getSystemService(
                        DevicePolicyManager.class);
                // Only allow provisioning if the device is not already provisioned
                if (provisioningMode == DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE) {
                    return devicePolicyManager.getPolicyManagedProfiles(
                            Process.myUserHandle()).size() == 0;
                } else if (provisioningMode
                        == DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE) {
                    return !devicePolicyManager.isDeviceOwnerAppOnAnyUser(BELLIS_PACKAGE);
                }
            }
        }
        return false;
    }
}
