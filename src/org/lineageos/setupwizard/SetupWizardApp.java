/*
 * SPDX-FileCopyrightText: 2013 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static android.os.UserHandle.USER_CURRENT;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;

import android.app.AppOpsManager;
import android.app.Application;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.ServiceManager;
import android.permission.PermissionManager;
import android.provider.Settings;
import android.service.oemlock.OemLockManager;
import android.util.Log;

import org.lineageos.setupwizard.util.SetupWizardUtils;

import java.util.List;

public class SetupWizardApp extends Application {

    public static final String TAG = SetupWizardApp.class.getSimpleName();
    // Verbose logging
    public static final boolean LOGV = Log.isLoggable(TAG, Log.VERBOSE);

    public static final String ACTION_ACCESSIBILITY_SETTINGS =
            "android.settings.ACCESSIBILITY_SETTINGS_FOR_SUW";
    public static final String ACTION_FINISHED = "org.lineageos.setupwizard.SETUP_FINISHED";
    public static final String ACTION_SETUP_COMPLETE =
            "org.lineageos.setupwizard.LINEAGE_SETUP_COMPLETE";
    public static final String ACTION_SETUP_NETWORK = "android.settings.NETWORK_PROVIDER_SETUP";
    public static final String ACTION_SETUP_BIOMETRIC = "android.settings.BIOMETRIC_ENROLL";
    public static final String ACTION_SETUP_LOCKSCREEN = "com.android.settings.SETUP_LOCK_SCREEN";
    public static final String ACTION_SETUP_INSTALL = "org.calyxos.lupin.INSTALL";
    public static final String ACTION_RESTORE_FROM_BACKUP =
            "com.stevesoltys.seedvault.RESTORE_BACKUP";
    public static final String ACTION_EMERGENCY_DIAL = "com.android.phone.EmergencyDialer.DIAL";
    public static final String ACTION_LOAD = "com.android.wizard.LOAD";

    public static final String EXTRA_SCRIPT_URI = "scriptUri";
    public static final String EXTRA_ACTION_ID = "actionId";
    public static final String EXTRA_RESULT_CODE = "com.android.setupwizard.ResultCode";
    public static final String EXTRA_PREFS_SHOW_BUTTON_BAR = "extra_prefs_show_button_bar";
    public static final String EXTRA_PREFS_SHOW_SKIP = "extra_prefs_show_skip";
    public static final String EXTRA_PREFS_SHOW_SKIP_TV = "extra_show_skip_network";
    public static final String EXTRA_PREFS_SET_BACK_TEXT = "extra_prefs_set_back_text";
    public static final String EXTRA_ENABLE_NEXT_ON_CONNECT = "wifi_enable_next_on_connect";

    public static final String AURORA_SERVICES_PACKAGE = "com.aurora.services";
    public static final String AURORA_STORE_PACKAGE = "com.aurora.store";
    public static final String FDROID_BASIC_PACKAGE = "org.fdroid.basic";
    public static final List<String> PACKAGE_INSTALLERS =
            List.of(FDROID_BASIC_PACKAGE, AURORA_STORE_PACKAGE);

    public static final String NAVIGATION_OPTION_KEY = "navigation_option";

    public static final int RADIO_READY_TIMEOUT = 10 * 1000;

    private static StatusBarManager sStatusBarManager;

    private boolean mIsRadioReady = false;
    private boolean mIgnoreSimLocale = false;

    private final Bundle mSettingsBundle = new Bundle();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final Runnable mRadioTimeoutRunnable = () -> mIsRadioReady = true;

    @Override
    public void onCreate() {
        super.onCreate();
        if (LOGV) {
            Log.v(TAG, "onCreate()");
        }
        SetupWizardUtils.disableComponentsForMissingFeatures(this);
        if (SetupWizardUtils.isOwner()) {
            SetupWizardUtils.setMobileDataEnabled(this, false);
        }
        sStatusBarManager = SetupWizardUtils.disableStatusBar(this);
        if (SetupWizardUtils.isPackageInstalled(this, NAV_BAR_MODE_GESTURAL_OVERLAY)) {
            IOverlayManager overlayManager = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(Context.OVERLAY_SERVICE));
            try {
                overlayManager.setEnabledExclusiveInCategory(NAV_BAR_MODE_GESTURAL_OVERLAY,
                    USER_CURRENT);
            } catch (Exception e) {}
        }
        mHandler.postDelayed(mRadioTimeoutRunnable, SetupWizardApp.RADIO_READY_TIMEOUT);
        // If the bootloader is locked, and OEM unlocking is allowed, turn it off
        if (SetupWizardUtils.isOwner()
                && !SetupWizardUtils.isBootloaderUnlocked(this)
                && SetupWizardUtils.isOemunlockAllowed(this)) {
            getSystemService(OemLockManager.class).setOemUnlockAllowedByUser(false);
        }
        try {
            getSystemService(AppOpsManager.class).setMode(AppOpsManager.OP_REQUEST_INSTALL_PACKAGES,
                    getPackageManager().getPackageUid(AURORA_SERVICES_PACKAGE, 0),
                    AURORA_SERVICES_PACKAGE,
                    AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (SetupWizardUtils.hasGMS(this)) {
            SetupWizardUtils.disableHome(this);
            if (SetupWizardUtils.isOwner()) {
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.ASSISTED_GPS_ENABLED, 1);
            }
        }
    }

    public static StatusBarManager getStatusBarManager() {
        return sStatusBarManager;
    }

    public boolean ignoreSimLocale() {
        return mIgnoreSimLocale;
    }

    public void setIgnoreSimLocale(boolean ignoreSimLocale) {
        mIgnoreSimLocale = ignoreSimLocale;
    }

    public Bundle getSettingsBundle() {
        return mSettingsBundle;
    }

    public void provisionDefaultUserAppPermissions() {
        for (String packageName : PACKAGE_INSTALLERS) {
            if (LOGV) Log.v(TAG, "Provisioning default permissions for " + packageName + "...");
            try {
                getSystemService(AppOpsManager.class).setMode(
                        AppOpsManager.OP_REQUEST_INSTALL_PACKAGES,
                        getPackageManager().getPackageUid(packageName, 0),
                        packageName,
                        AppOpsManager.MODE_ALLOWED);
            } catch (PackageManager.NameNotFoundException ignored) {
                if (LOGV) Log.v(TAG, "Missing " + packageName + " for appops, skipping");
                continue;
            } catch (Exception e) {
                Log.e(TAG, "Failed to grant install unknown apps permission to " + packageName, e);
            }
            try {
                getSystemService(PermissionManager.class).grantRuntimePermission(packageName,
                        android.Manifest.permission.POST_NOTIFICATIONS,
                        Process.myUserHandle());
            } catch (Exception e) {
                Log.e(TAG, "Failed to grant post notifications permission to " + packageName, e);
            }
        }
    }
}
