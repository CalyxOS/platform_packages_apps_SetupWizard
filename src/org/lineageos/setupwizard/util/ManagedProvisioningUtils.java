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

import static android.app.admin.DevicePolicyManager.STATE_USER_PROFILE_COMPLETE;
import static android.app.admin.DevicePolicyManager.STATE_USER_PROFILE_FINALIZED;

import static org.lineageos.setupwizard.SetupWizardApp.LOGV;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.os.Process;
import android.util.Log;

import lineageos.providers.LineageSettings;

import org.lineageos.setupwizard.R;

public class ManagedProvisioningUtils {

    private static final String TAG = ManagedProvisioningUtils.class.getSimpleName();
    private static final String PREF_WIPE_REQUIRED = "wipe_required";

    public static final int GARLIC_LEVEL_STANDARD = 0;
    public static final int GARLIC_LEVEL_SAFER = 1;
    public static final int GARLIC_LEVEL_SAFEST = 2;
    public static final int GARLIC_LEVEL_DEFAULT = GARLIC_LEVEL_STANDARD;

    private static final String BELLIS_PACKAGE = "org.calyxos.bellis";
    private static final String BELLIS_DEVICE_ADMIN_RECEIVER_CLASS = ".BasicDeviceAdminReceiver";

    public enum ProvisioningState {
        UNSUPPORTED,
        PENDING,
        COMPLETE,
        FINALIZED
    }

    public static Intent getFinalizeProvisioningIntent(final @NonNull Context context) {
        if (LOGV) {
            Log.v(TAG, "finalizeProvisioning");
        }
        return new Intent(DevicePolicyManager.ACTION_PROVISION_FINALIZATION);
    }

    public static Intent getStartProvisioningIntent(final @NonNull Context context) {
        final PersistableBundle persistableBundle = new PersistableBundle();
        final Integer provisioningMode = getProvisioningMode(context);
        if (provisioningMode == null) {
            throw new UnsupportedOperationException("provisioningMode is null");
        }
        persistableBundle.putInt(DevicePolicyManager.EXTRA_PROVISIONING_MODE,
                provisioningMode.intValue());
        persistableBundle.putInt(LineageSettings.Global.GARLIC_LEVEL, getGarlicLevel(context));

        final Intent intent = new Intent(
                DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE)
                .putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                        new ComponentName(BELLIS_PACKAGE, BELLIS_PACKAGE
                                + BELLIS_DEVICE_ADMIN_RECEIVER_CLASS))
                .putExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
                        persistableBundle)
                .putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS, true);
        if (provisioningMode.equals(DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE)) {
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED,
                    true);
        }
        return intent;
    }

    private static Integer getProvisioningMode(final @NonNull Context context) {
        switch (getGarlicLevel(context)) {
            case GARLIC_LEVEL_STANDARD:
                return null;
            default:
                return DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE;
        }
    }

    public static int getGarlicLevel(final @NonNull Context context) {
        return LineageSettings.Global.getInt(context.getContentResolver(),
                LineageSettings.Global.GARLIC_LEVEL, GARLIC_LEVEL_DEFAULT);
    }

    public static ProvisioningState getProvisioningState(final @NonNull Context context) {
        // Only system user can provision the device
        if (!SetupWizardUtils.isOwner()) {
            return ProvisioningState.UNSUPPORTED;
        }

        final Integer provisioningMode = getProvisioningMode(context);
        if (provisioningMode != null) {
            final DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
            if (provisioningMode.equals(DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE)) {
                // In managed profile provisioning mode, return PENDING with no policy-managed
                // profiles yet; otherwise, return either COMPLETE or FINALIZED.
                if (dpm.getPolicyManagedProfiles(Process.myUserHandle()).size() == 0) {
                    return ProvisioningState.PENDING;
                }
                final int userProvisioningState = dpm.getUserProvisioningState();
                Log.v(TAG, "getProvisioningState: " + userProvisioningState);
                switch (userProvisioningState) {
                    case STATE_USER_PROFILE_COMPLETE:
                        return ProvisioningState.COMPLETE;
                    case STATE_USER_PROFILE_FINALIZED:
                        return ProvisioningState.FINALIZED;
                }
            } else if (provisioningMode
                    .equals(DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE)) {
                // NOTE: THIS MODE IS NOT CURRENTLY UTILIZED. THIS CODE MAY NEED TO BE UPDATED
                // FOR IT TO BE PROPERLY UTILIZED IN THE FUTURE. THE HANDLING ABOVE MAY BE USEFUL.
                // In fully-managed mode, if Bellis is not the device owner yet, return PENDING;
                // otherwise, return COMPLETE.
                return !dpm.isDeviceOwnerAppOnAnyUser(BELLIS_PACKAGE)
                        ? ProvisioningState.PENDING : ProvisioningState.COMPLETE;
            }
        }
        return ProvisioningState.UNSUPPORTED;
    }

    public static boolean maybeShowFailedProvisioningDialogAgain(Activity activity) {
        if (SetupWizardUtils.getPrefs(activity).getBoolean(PREF_WIPE_REQUIRED, false)) {
            showFailedProvisioningDialog(activity);
            return true;
        }
        return false;
    }

    public static void showFailedProvisioningDialog(Activity activity) {
        SetupWizardUtils.getPrefs(activity).edit()
                .putBoolean(PREF_WIPE_REQUIRED, true)
                .apply();
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity).setTitle(
                R.string.cant_set_up_device).setMessage(
                R.string.provisioning_failed).setPositiveButton(R.string.reset,
                (dialogInterface, i) -> activity.getSystemService(
                        DevicePolicyManager.class).wipeDevice(0)).setCancelable(
                false).show());
    }
}
