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

import static org.lineageos.setupwizard.SetupWizardApp.LOGV;

import android.annotation.NonNull;
import android.annotation.Nullable;
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
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

import lineageos.providers.LineageSettings;

public class ManagedProvisioningUtils {

    private static final String TAG = ManagedProvisioningUtils.class.getSimpleName();

    public static final int GARLIC_LEVEL_STANDARD = 0;
    public static final int GARLIC_LEVEL_SAFER = 1;
    public static final int GARLIC_LEVEL_SAFEST = 2;
    public static final int GARLIC_LEVEL_DEFAULT = GARLIC_LEVEL_STANDARD;

    private static final String BELLIS_PACKAGE = "org.calyxos.bellis";
    private static final String BELLIS_DEVICE_ADMIN_RECEIVER_CLASS = ".BasicDeviceAdminReceiver";

    private static final String ORBOT_PACKAGE = "org.torproject.android";
    private static final String ORBOT_APK_PATH = "/product/fdroid/repo/Orbot.apk";

    public enum ProvisioningState {
        UNSUPPORTED,
        PENDING,
        COMPLETE
    }

    public static void init(final @NonNull Context context) {
        startProvisioning(context);
    }

    public static void finalizeProvisioning(final @NonNull Context context,
            final @Nullable Consumer<Exception> completionConsumer) {
        if (LOGV) {
            Log.v(TAG, "finalizeProvisioning");
        }
        Context userContext = context;
        for (UserHandle userHandle : UserManager.get(context).getUserHandles(false)) {
            userContext = context.createContextAsUser(userHandle, 0);
        }
        // userContext, now the Context for the last-listed user, is reasonably assumed to be that
        // of the newly-created managed profile.
        IntentSender intentSender = new IntentSender((IIntentSender) new IIntentSender.Stub() {
            @Override
            public void send(int code, Intent intent, String resolvedType,
                    IBinder whitelistToken, IIntentReceiver finishedReceiver,
                    String requiredPermission, Bundle options) {
                final int status = intent.getIntExtra(
                        PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
                if (status != PackageInstaller.STATUS_SUCCESS) {
                    final String errorMessage = "Failed to install "
                            + intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME) + "["
                            + intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) + "]";
                    Log.e(TAG, errorMessage);
                    if (completionConsumer != null) {
                        completionConsumer.accept(new Exception(errorMessage));
                    }
                    return;
                }

                // Finalize provisioning in Bellis.
                if (LOGV) {
                    Log.v(TAG, "finalizeProvisioning: Starting Bellis...");
                }
                context.startActivity(new Intent(DevicePolicyManager.ACTION_PROVISION_FINALIZATION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

                if (completionConsumer != null) {
                    // Return success via null Exception.
                    completionConsumer.accept(null);
                }
            }
        });
        installApk(userContext, intentSender, ORBOT_APK_PATH);
    }

    private static void installApk(Context context, IntentSender intentSender, String apkPath) {
        try {
            PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
            PackageInstaller.Session session = packageInstaller.openSession(
                    packageInstaller.createSession(new PackageInstaller.SessionParams(
                            PackageInstaller.SessionParams.MODE_FULL_INSTALL)));
            try (OutputStream packageInSession = session.openWrite("package", 0, -1);
                 InputStream is = new FileInputStream(apkPath)) {
                byte[] buffer = new byte[16384];
                int n;
                while ((n = is.read(buffer)) >= 0) {
                    packageInSession.write(buffer, 0, n);
                }
            }
            session.commit(intentSender);
        } catch (IOException e) {
            Log.e(TAG, "Failed to install " + apkPath, e);
        }
    }

    private static void startProvisioning(final @NonNull Context context) {
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
        context.startActivityForResult("", intent, 0, null);
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
                // In managed profile provisioning mode, having no policy-managed profiles yet
                // return PENDING; otherwise, return COMPLETE.
                return dpm.getPolicyManagedProfiles(Process.myUserHandle()).size() == 0
                        ? ProvisioningState.PENDING : ProvisioningState.COMPLETE;
            } else if (provisioningMode
                    .equals(DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE)) {
                // In fully-managed mode, if Bellis is not the device owner yet, return PENDING;
                // otherwise, return COMPLETE.
                return !dpm.isDeviceOwnerAppOnAnyUser(BELLIS_PACKAGE)
                        ? ProvisioningState.PENDING : ProvisioningState.COMPLETE;
            }
        }
        return ProvisioningState.UNSUPPORTED;
    }
}
