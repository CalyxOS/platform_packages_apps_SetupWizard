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
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;

import org.lineageos.setupwizard.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lineageos.providers.LineageSettings;

public class ManagedProvisioningUtils {

    private static final String TAG = ManagedProvisioningUtils.class.getSimpleName();

    private static final String BELLIS_PACKAGE = "org.calyxos.bellis";
    private static final String BELLIS_DEVICE_ADMIN_RECEIVER_CLASS = ".BasicDeviceAdminReceiver";

    private static final String ORBOT_PACKAGE = "org.torproject.android";
    private static final String ORBOT_APK_LOCATION = "/product/fdroid/repo/Orbot.apk";

    private enum GarlicLevel {
        STANDARD,
        SAFER,
        SAFEST
    }

    public static void init(Context context) {
        final int garlicLevel = LineageSettings.Global.getInt(context.getContentResolver(),
                LineageSettings.Global.GARLIC_LEVEL, 0);
        if (garlicLevel == GarlicLevel.SAFER.ordinal()) {
            setupSaferMode(context);
        } else if (garlicLevel == GarlicLevel.SAFEST.ordinal()) {
            setupSafestMode(context);
        }
        startProvisioning(context);
    }

    public static void finalize(Context context) {
        installOrbot(context);
        context.startActivity(new Intent(DevicePolicyManager.ACTION_PROVISION_FINALIZATION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private static void installOrbot(Context context) {
        try {
            // Get PackageInstaller for the managed profile owned by the current user
            // to install Orbot directly within the profile
            PackageInstaller packageInstaller = context.createContextAsUser(
                    context.getSystemService(DevicePolicyManager.class).getPolicyManagedProfiles(
                            Process.myUserHandle()).get(0), 0)
                    .getPackageManager().getPackageInstaller();
            PackageInstaller.Session session = packageInstaller.openSession(
                    packageInstaller.createSession(new PackageInstaller.SessionParams(
                            PackageInstaller.SessionParams.MODE_FULL_INSTALL)));
            try (OutputStream packageInSession = session.openWrite("package", 0, -1);
                 InputStream is = new FileInputStream(ORBOT_APK_LOCATION)) {
                byte[] buffer = new byte[16384];
                int n;
                while ((n = is.read(buffer)) >= 0) {
                    packageInSession.write(buffer, 0, n);
                }
            }
            final IIntentSender.Stub mLocalSender = new IIntentSender.Stub() {
                @Override
                public void send(int code, Intent intent, String resolvedType,
                        IBinder whitelistToken, IIntentReceiver finishedReceiver,
                        String requiredPermission, Bundle options) {
                    final int status = intent.getIntExtra(
                            PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
                    if (status != PackageInstaller.STATUS_SUCCESS) {
                        Log.e(TAG, "Failed to install Orbot [" +
                                intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) + "]");
                    }
                }
            };
            session.commit(new IntentSender((IIntentSender) mLocalSender));
        } catch (IOException e) {
            Log.e(TAG, "Failed to install Orbot", e);
        }
    }

    /**
     * Applies a pre-defined safer mode configuration
     *
     * Bluetooth Timeout: 15 seconds
     * WiFi Timeout: 15 seconds
     * Device Auto-reboot Timeout: 36 hours
     * @param context Current context
     */
    private static void setupSaferMode(Context context) {
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.BLUETOOTH_OFF_TIMEOUT, 15000);
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.WIFI_OFF_TIMEOUT, 15000);
        LineageSettings.Global.putInt(context.getContentResolver(),
                LineageSettings.Global.DEVICE_REBOOT_TIMEOUT, 3600000);
    }

    /**
     * Applies a pre-defined safest mode configuration
     *
     * Bluetooth Timeout: 15 seconds
     * WiFi Timeout: 15 seconds
     * Device Auto-reboot Timeout: 36 hours
     * @param context Current context
     */
    private static void setupSafestMode(Context context) {
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.BLUETOOTH_OFF_TIMEOUT, 15000);
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.WIFI_OFF_TIMEOUT, 15000);
        LineageSettings.Global.putInt(context.getContentResolver(),
                LineageSettings.Global.DEVICE_REBOOT_TIMEOUT, 3600000);
    }

    private static void startProvisioning(Context context) {
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
