/*
 * SPDX-FileCopyrightText: 2013 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.util;

import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;
import static android.content.pm.PackageManager.GET_ACTIVITIES;
import static android.os.UserHandle.USER_CURRENT;
import static android.telephony.TelephonyManager.PHONE_TYPE_GSM;

import static com.android.internal.telephony.PhoneConstants.LTE_ON_CDMA_TRUE;
import static com.android.internal.telephony.PhoneConstants.LTE_ON_CDMA_UNKNOWN;

import static com.google.android.setupcompat.util.ResultCodes.RESULT_SKIP;

import static org.lineageos.setupwizard.SetupWizardApp.LOGV;
import static org.lineageos.setupwizard.SetupWizardApp.NAVIGATION_OPTION_KEY;
import static org.lineageos.setupwizard.SetupWizardApp.PACKAGE_INSTALLERS;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.StatusBarManager;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.om.IOverlayManager;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.permission.PermissionManager;
import android.provider.Settings;
import android.service.oemlock.OemLockManager;
import android.sysprop.TelephonyProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.setupcompat.util.WizardManagerHelper;

import org.lineageos.setupwizard.BaseSetupWizardActivity;
import org.lineageos.setupwizard.BiometricActivity;
import org.lineageos.setupwizard.BluetoothSetupActivity;
import org.lineageos.setupwizard.BootloaderWarningActivity;
import org.lineageos.setupwizard.NetworkSetupActivity;
import org.lineageos.setupwizard.ScreenLockActivity;
import org.lineageos.setupwizard.SetupWizardActivity;
import org.lineageos.setupwizard.SetupWizardApp;
import org.lineageos.setupwizard.util.ManagedProvisioningUtils.ProvisioningState;

import java.util.List;

public class SetupWizardUtils {

    private static final String TAG = SetupWizardUtils.class.getSimpleName();

    private static final String GMS_PACKAGE = "com.google.android.gms";
    private static final String GMS_SUW_PACKAGE = "com.google.android.setupwizard";
    private static final String GMS_TV_SUW_PACKAGE = "com.google.android.tungsten.setupwraith";

    private static final String PROP_BUILD_DATE = "ro.build.date.utc";

    private SetupWizardUtils() {
    }

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences("SetupWizardPrefs", MODE_PRIVATE);
    }

    public static boolean hasWifi(Context context) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI);
    }

    public static boolean hasTelephony(Context context) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    public static boolean isOwner() {
        return UserHandle.myUserId() == 0;
    }

    public static boolean isManagedProfile(Context context) {
        return context.getSystemService(UserManager.class).isManagedProfile();
    }

    public static StatusBarManager disableStatusBar(Context context) {
        StatusBarManager statusBarManager = context.getSystemService(StatusBarManager.class);
        if (statusBarManager != null) {
            if (LOGV) {
                Log.v(SetupWizardApp.TAG, "Disabling status bar");
            }
            statusBarManager.setDisabledForSetup(true);
        } else {
            Log.w(SetupWizardApp.TAG,
                    "Skip disabling status bar - could not get StatusBarManager");
        }
        return statusBarManager;
    }

    public static void enableStatusBar() {
        StatusBarManager statusBarManager = SetupWizardApp.getStatusBarManager();
        if (statusBarManager != null) {
            if (LOGV) {
                Log.v(SetupWizardApp.TAG, "Enabling status bar");
            }
            statusBarManager.setDisabledForSetup(false);

            // Session must be destroyed if it's not used anymore
            statusBarManager = null;
        } else {
            Log.w(SetupWizardApp.TAG,
                    "Skip enabling status bar - could not get StatusBarManager");
        }
    }

    public static boolean hasGMS(Context context) {
        String gmsSuwPackage = hasLeanback(context) ? GMS_TV_SUW_PACKAGE : GMS_SUW_PACKAGE;

        if (isPackageInstalled(context, GMS_PACKAGE) &&
                isPackageInstalled(context, gmsSuwPackage)) {
            PackageManager packageManager = context.getPackageManager();
            if (LOGV) {
                Log.v(TAG, GMS_SUW_PACKAGE + " state = " +
                        packageManager.getApplicationEnabledSetting(gmsSuwPackage));
            }
            return packageManager.getApplicationEnabledSetting(gmsSuwPackage) !=
                    COMPONENT_ENABLED_STATE_DISABLED;
        }
        return false;
    }

    public static boolean isPackageInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void finishSetupWizard(BaseSetupWizardActivity context) {
        if (LOGV) {
            Log.v(TAG, "finishSetupWizard");
        }
        ProvisioningState provisioningState =
                ManagedProvisioningUtils.getProvisioningState(context);
        if (LOGV) {
            Log.v(TAG, "finishSetupWizard, provisioningState=" + provisioningState);
        }
        if (provisioningState == ProvisioningState.PENDING) {
            Log.e(TAG, "finishSetupWizard, but provisioning pending! Murky waters ahead!");
        }
        ContentResolver contentResolver = context.getContentResolver();
        Settings.Global.putInt(contentResolver,
                Settings.Global.DEVICE_PROVISIONED, 1);
        final int userSetupComplete =
                Settings.Secure.getInt(contentResolver, Settings.Secure.USER_SETUP_COMPLETE, 0);
        if (userSetupComplete != 0 && !SetupWizardUtils.isManagedProfile(context)) {
            Log.e(TAG, "finishSetupWizard, but userSetupComplete=" + userSetupComplete + "! "
                    + "This should not happen!");
        }
        Settings.Secure.putInt(contentResolver,
                Settings.Secure.USER_SETUP_COMPLETE, 1);
        if (hasLeanback(context)) {
            Settings.Secure.putInt(contentResolver,
                    Settings.Secure.TV_USER_SETUP_COMPLETE, 1);
        }
        if (userSetupComplete != 1 && provisioningState == ProvisioningState.COMPLETE) {
            ManagedProvisioningUtils.finalizeProvisioning(context, result -> {
                // TODO: handle failure result
            });
        }

        handleNavigationOption();
        provisionDefaultUserAppPermissions(context);
        sendMicroGCheckInBroadcast(context);
        WallpaperManager.getInstance(context).forgetLoadedWallpaper();
        disableHome(context);
        enableStatusBar();
        context.finishAffinity();
        context.nextAction(RESULT_SKIP);
        Log.i(TAG, "Setup complete!");
    }

    public static boolean isSetupWizardComplete(Context context) {
        if (!isManagedProfile(context) && WizardManagerHelper.isUserSetupComplete(context)) {
            return true;
        }
        final int enabledSetting = context.getPackageManager().getComponentEnabledSetting(
                new ComponentName(context, SetupWizardActivity.class));
        return switch (enabledSetting) {
            case COMPONENT_ENABLED_STATE_DEFAULT, COMPONENT_ENABLED_STATE_ENABLED -> false;
            default -> true;
        };
    }

    public static boolean isBluetoothDisabled() {
        return SystemProperties.getBoolean("config.disable_bluetooth", false);
    }

    public static boolean isEthernetConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (networkCapabilities != null) {
            return networkCapabilities.hasCapability(NetworkCapabilities.TRANSPORT_ETHERNET);
        }
        return false;
    }

    public static boolean hasLeanback(Context context) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    public static boolean hasBiometric(Context context) {
        BiometricManager biometricManager = context.getSystemService(BiometricManager.class);
        int result = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK);
        return switch (result) {
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
                    BiometricManager.BIOMETRIC_SUCCESS -> true;
            default -> false;
        };
    }

    public static boolean isBootloaderUnlocked(Context context) {
        OemLockManager oemLockManager = context.getSystemService(OemLockManager.class);
        if (oemLockManager != null) {
            return oemLockManager.isDeviceOemUnlocked();
        }
        return true; // Default to unlocked
    }

    public static boolean isOemunlockAllowed(Context context) {
        OemLockManager oemLockManager = context.getSystemService(OemLockManager.class);
        if (oemLockManager != null) {
            return oemLockManager.isOemUnlockAllowed();
        }
        return true; // Default to unlock allowed
    }

    public static void disableComponentsForMissingFeatures(Context context) {
        if (!hasLeanback(context) || isBluetoothDisabled()) {
            disableComponent(context, BluetoothSetupActivity.class);
        }
        if (!hasBiometric(context)) {
            disableComponent(context, BiometricActivity.class);
        } else {
            disableComponent(context, ScreenLockActivity.class);
        }
        if ((!hasWifi(context) && !hasTelephony(context)) || isEthernetConnected(context)) {
            disableComponent(context, NetworkSetupActivity.class);
        }
        if (!isBootloaderUnlocked(context) || Build.IS_DEBUGGABLE) {
            disableComponent(context, BootloaderWarningActivity.class);
        }
    }

    /**
     * Disable the Home component, which is presumably SetupWizardActivity at this time.
     */
    public static void disableHome(Context context) {
        ComponentName homeComponent = getHomeComponent(context);
        if (homeComponent != null) {
            setComponentEnabledState(context, homeComponent, COMPONENT_ENABLED_STATE_DISABLED);
        } else {
            Log.w(TAG, "Home component not found. Skipping.");
        }
    }

    private static ComponentName getHomeComponent(Context context) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        intent.setPackage(context.getPackageName());
        ComponentName comp = intent.resolveActivity(context.getPackageManager());
        if (LOGV) {
            Log.v(TAG, "resolveActivity for intent=" + intent + " returns " + comp);
        }
        return comp;
    }

    public static void disableComponent(Context context, Class<?> cls) {
        setComponentEnabledState(context, new ComponentName(context, cls),
                COMPONENT_ENABLED_STATE_DISABLED);
    }

    public static void enableComponent(Context context, Class<?> cls) {
        setComponentEnabledState(context, new ComponentName(context, cls),
                COMPONENT_ENABLED_STATE_ENABLED);
    }

    public static void setComponentEnabledState(Context context, ComponentName componentName,
            int enabledState) {
        context.getPackageManager().setComponentEnabledSetting(componentName,
                enabledState, DONT_KILL_APP);
    }

    private static void sendMicroGCheckInBroadcast(Context context) {
        Intent i = new Intent("android.server.checkin.CHECKIN");
        i.setPackage(GMS_PACKAGE);
        context.sendBroadcast(i);
    }

    private static void handleNavigationOption() {
        Bundle settingsBundle = SetupWizardApp.getSettingsBundle();
        if (settingsBundle.containsKey(NAVIGATION_OPTION_KEY)) {
            IOverlayManager overlayManager = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(Context.OVERLAY_SERVICE));
            String selectedNavMode = settingsBundle.getString(NAVIGATION_OPTION_KEY);

            try {
                overlayManager.setEnabledExclusiveInCategory(selectedNavMode, USER_CURRENT);
            } catch (Exception ignored) {
            }
        }
    }

    private static void provisionDefaultUserAppPermissions(Context context) {
        for (String packageName : PACKAGE_INSTALLERS) {
            if (LOGV) Log.v(TAG, "Provisioning default permissions for " + packageName + "...");
            try {
                context.getSystemService(AppOpsManager.class).setMode(
                        AppOpsManager.OP_REQUEST_INSTALL_PACKAGES,
                        context.getPackageManager().getPackageUid(packageName, 0),
                        packageName,
                        AppOpsManager.MODE_ALLOWED);
            } catch (PackageManager.NameNotFoundException ignored) {
                if (LOGV) Log.v(TAG, "Missing " + packageName + " for appops, skipping");
                continue;
            } catch (Exception e) {
                Log.e(TAG, "Failed to grant install unknown apps permission to " + packageName, e);
            }
            try {
                context.getSystemService(PermissionManager.class).grantRuntimePermission(
                        packageName,
                        android.Manifest.permission.POST_NOTIFICATIONS,
                        Process.myUserHandle());
            } catch (Exception e) {
                Log.e(TAG, "Failed to grant post notifications permission to " + packageName, e);
            }
        }
    }

    public static long getBuildDateTimestamp() {
        return SystemProperties.getLong(PROP_BUILD_DATE, 0);
    }

    public static boolean simMissing(Context context) {
        TelephonyManager tm = context.getSystemService(TelephonyManager.class);
        SubscriptionManager sm = context.getSystemService(SubscriptionManager.class);
        if (tm == null || sm == null) {
            return false;
        }
        List<SubscriptionInfo> subs = sm.getActiveSubscriptionInfoList();
        if (subs != null) {
            for (SubscriptionInfo sub : subs) {
                int simState = tm.getSimState(sub.getSimSlotIndex());
                if (LOGV) {
                    Log.v(TAG, "getSimState(" + sub.getSubscriptionId() + ") == " + simState);
                }
                if (simState != -1) {
                    final int subId = sub.getSubscriptionId();
                    final TelephonyManager subTm = tm.createForSubscriptionId(subId);
                    if (isGSM(subTm) || isLteOnCdma(subTm, subId)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isGSM(TelephonyManager subTelephonyManager) {
        return subTelephonyManager.getCurrentPhoneType() == PHONE_TYPE_GSM;
    }

    private static boolean isLteOnCdma(TelephonyManager subTelephonyManager, int subId) {
        final int lteOnCdmaMode = subTelephonyManager.getLteOnCdmaMode(subId);
        if (lteOnCdmaMode == LTE_ON_CDMA_UNKNOWN) {
            return TelephonyProperties.lte_on_cdma_device().orElse(LTE_ON_CDMA_UNKNOWN)
                    == LTE_ON_CDMA_TRUE;
        }
        return lteOnCdmaMode == LTE_ON_CDMA_TRUE;
    }
}
