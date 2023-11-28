/*
 * Copyright (C) 2013 The CyanogenMod Project
 * Copyright (C) 2017-2022 The LineageOS Project
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

package org.lineageos.setupwizard;

import static android.os.UserHandle.USER_CURRENT;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;

import android.app.AppOpsManager;
import android.app.Application;
import android.app.StatusBarManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.service.oemlock.OemLockManager;
import android.util.Log;

import java.util.List;

import org.lineageos.setupwizard.util.NetworkMonitor;
import org.lineageos.setupwizard.util.PhoneMonitor;
import org.lineageos.setupwizard.util.SetupWizardUtils;

public class SetupWizardApp extends Application {

    public static final String TAG = SetupWizardApp.class.getSimpleName();
    // Verbose logging
    public static final boolean LOGV = Log.isLoggable(TAG, Log.VERBOSE);

    public static final String ACTION_ACCESSIBILITY_SETTINGS =
            "android.settings.ACCESSIBILITY_SETTINGS_FOR_SUW";
    public static final String ACTION_SETUP_COMPLETE =
            "org.lineageos.setupwizard.LINEAGE_SETUP_COMPLETE";
    public static final String ACTION_FINISHED = "org.lineageos.setupwizard.SETUP_FINISHED";
    public static final String ACTION_SETUP_NETWORK = "android.settings.NETWORK_PROVIDER_SETUP";
    public static final String ACTION_APPS_INSTALLED =
            "org.lineageos.setupwizard.LINEAGE_APPS_INSTALLED";
    public static final String ACTION_SETUP_BIOMETRIC = "android.settings.BIOMETRIC_ENROLL";
    public static final String ACTION_SETUP_LOCKSCREEN = "com.android.settings.SETUP_LOCK_SCREEN";
    public static final String ACTION_SETUP_INSTALL = "org.calyxos.lupin.INSTALL";
    public static final String ACTION_RESTORE_FROM_BACKUP =
            "com.stevesoltys.seedvault.RESTORE_BACKUP";
    public static final String ACTION_EMERGENCY_DIAL = "com.android.phone.EmergencyDialer.DIAL";
    public static final String ACTION_NEXT = "com.android.wizard.NEXT";
    public static final String ACTION_LOAD = "com.android.wizard.LOAD";

    public static final String EXTRA_HAS_MULTIPLE_USERS = "hasMultipleUsers";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_DETAILS = "details";
    public static final String EXTRA_SCRIPT_URI = "scriptUri";
    public static final String EXTRA_ACTION_ID = "actionId";
    public static final String EXTRA_RESULT_CODE = "com.android.setupwizard.ResultCode";
    public static final String EXTRA_PREFS_SHOW_BUTTON_BAR = "extra_prefs_show_button_bar";
    public static final String EXTRA_PREFS_SHOW_SKIP = "extra_prefs_show_skip";
    public static final String EXTRA_PREFS_SHOW_SKIP_TV = "extra_show_skip_network";
    public static final String EXTRA_PREFS_SET_BACK_TEXT = "extra_prefs_set_back_text";
    public static final String EXTRA_ENABLE_NEXT_ON_CONNECT = "wifi_enable_next_on_connect";

    public static final String KEY_DETECT_CAPTIVE_PORTAL = "captive_portal_detection_enabled";

    public static final String AURORA_SERVICES_PACKAGE = "com.aurora.services";
    public static final String AURORA_STORE_PACKAGE = "com.aurora.store";
    public static final String FDROID_BASIC_PACKAGE = "org.fdroid.basic";
    public static final List<String> PACKAGE_INSTALLERS =
            List.of(FDROID_BASIC_PACKAGE, AURORA_STORE_PACKAGE);

    public static final String NAVIGATION_OPTION_KEY = "navigation_option";

    public static final int REQUEST_CODE_SETUP_NETWORK = 0;
    public static final int REQUEST_CODE_SETUP_CAPTIVE_PORTAL = 4;
    public static final int REQUEST_CODE_SETUP_BLUETOOTH = 5;
    public static final int REQUEST_CODE_SETUP_BIOMETRIC = 7;
    public static final int REQUEST_CODE_SETUP_LOCKSCREEN = 9;
    public static final int REQUEST_CODE_SETUP_INSTALL = 10;
    public static final int REQUEST_CODE_RESTORE = 11;
    public static final int REQUEST_CODE_SETUP_EUICC = 12;

    public static final int RADIO_READY_TIMEOUT = 10 * 1000;

    private static StatusBarManager sStatusBarManager;

    private boolean mIsRadioReady = false;
    private boolean mIgnoreSimLocale = false;

    private final Bundle mSettingsBundle = new Bundle();
    private final Handler mHandler = new Handler();

    private final Runnable mRadioTimeoutRunnable = () -> mIsRadioReady = true;

    @Override
    public void onCreate() {
        super.onCreate();
        if (LOGV) {
            Log.v(TAG, "onCreate()");
        }
        NetworkMonitor.initInstance(this);
        PhoneMonitor.initInstance(this);
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
    }

    public static StatusBarManager getStatusBarManager() {
        return sStatusBarManager;
    }

    public boolean isRadioReady() {
        return mIsRadioReady;
    }

    public void setRadioReady(boolean radioReady) {
        if (!mIsRadioReady && radioReady) {
            mHandler.removeCallbacks(mRadioTimeoutRunnable);
        }
        mIsRadioReady = radioReady;
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
        }
    }
}
