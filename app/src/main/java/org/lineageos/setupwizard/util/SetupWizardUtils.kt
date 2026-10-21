/*
 * SPDX-FileCopyrightText: 2013 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.util

import android.Manifest
import android.app.AppOpsManager
import android.app.StatusBarManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.om.IOverlayManager
import android.content.pm.PackageManager
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import android.content.pm.PackageManager.DONT_KILL_APP
import android.content.pm.PackageManager.GET_ACTIVITIES
import android.hardware.biometrics.BiometricManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.PowerExemptionManager
import android.os.ServiceManager
import android.os.SystemProperties
import android.os.UserHandle
import android.os.UserManager
import android.permission.PermissionManager
import android.provider.Settings
import android.service.oemlock.OemLockManager
import android.service.persistentdata.PersistentDataBlockManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.PHONE_TYPE_GSM
import android.util.Log
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY
import com.android.internal.accessibility.util.AccessibilityUtils
import com.google.android.setupcompat.util.ResultCodes.RESULT_SKIP
import org.json.JSONObject
import org.lineageos.setupwizard.LOGV
import org.lineageos.setupwizard.NAVIGATION_OPTION_KEY
import org.lineageos.setupwizard.SetupWizardActivity
import org.lineageos.setupwizard.SetupWizardApp
import org.lineageos.setupwizard.base.BaseSetupWizardActivity
import org.lineageos.setupwizard.privacy.LocationProvider
import org.lineageos.setupwizard.util.ManagedProvisioningUtils.ProvisioningState
import org.lineageos.setupwizard.util.ManagedProvisioningUtils.finalizeProvisioning
import org.lineageos.setupwizard.util.ManagedProvisioningUtils.getProvisioningState

object SetupWizardUtils {

    private const val TAG = "SetupWizardUtils"

    private const val OVERLAY_CATEGORY_ACCENT_COLOR = "android.theme.customization.accent_color"
    private const val OVERLAY_CATEGORY_SYSTEM_PALETTE = "android.theme.customization.system_palette"
    private const val OVERLAY_COLOR_SOURCE = "android.theme.customization.color_source"
    private const val OVERLAY_COLOR_INDEX = "android.theme.customization.color_index"
    private const val COLOR_SOURCE_PRESET = "preset"
    private const val COLOR_SOURCE_HOME = "home_wallpaper"

    private const val GMS_PACKAGE = "com.google.android.gms"
    private const val GMS_FW_PACKAGE = "com.google.android.gsf"
    private const val GMS_SUW_PACKAGE = "com.google.android.setupwizard"
    private const val GMS_TV_SUW_PACKAGE = "com.google.android.tungsten.setupwraith"
    private const val UPDATER_PACKAGE = "org.lineageos.updater"

    private const val PROP_BUILD_DATE = "ro.build.date.utc"

    private const val AURORA_STORE_PACKAGE = "com.aurora.store"
    private const val FDROID_BASIC_PACKAGE = "org.fdroid.basic"
    private const val PLAY_STORE_PACKAGE = "com.android.vending"

    private val PACKAGE_INSTALLERS = listOf(FDROID_BASIC_PACKAGE, AURORA_STORE_PACKAGE)

    private const val TALKBACK_PACKAGE = "org.calyxos.talkback"
    private const val TALKBACK_SERVICE_CLASS = "com.google.android.marvin.talkback.TalkBackService"

    private const val MICROG_PROVISION_SERVICE = "org.microg.gms.provision.ProvisionService"
    const val MICROG_SERVICE_ENABLED = "microg_service_enabled"
    const val MICROG_NETWORK_LOCATION = "microg_network_location"
    const val MICROG_PUSH_NOTIFCATIONS = "microg_push_notifcations"
    const val MICROG_LOCATION_PROVIDER = "microg_location_provider"

    fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences("SetupWizardPrefs", MODE_PRIVATE)

    fun hasWifi(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)

    fun hasTelephony(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

    fun isOwner(): Boolean = UserHandle.myUserId() == 0

    fun isManagedProfile(context: Context): Boolean =
        context.getSystemService(UserManager::class.java).isManagedProfile()

    fun disableStatusBar(context: Context): StatusBarManager? {
        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
        statusBarManager?.also {
            if (LOGV) {
                Log.v(SetupWizardApp.TAG, "Disabling status bar")
            }
            it.setDisabledForSetup(true)
        } ?: Log.w(SetupWizardApp.TAG, "Skip disabling status bar - could not get StatusBarManager")
        return statusBarManager
    }

    fun enableStatusBar() {
        SetupWizardApp.statusBarManager?.also {
            if (LOGV) {
                Log.v(SetupWizardApp.TAG, "Enabling status bar")
            }
            it.setDisabledForSetup(false)
        } ?: Log.w(SetupWizardApp.TAG, "Skip enabling status bar - could not get StatusBarManager")
    }

    fun hasGMS(context: Context): Boolean {
        val gmsSuwPackage = if (hasLeanback(context)) GMS_TV_SUW_PACKAGE else GMS_SUW_PACKAGE
        if (
            isPackageInstalled(context, GMS_PACKAGE) && isPackageInstalled(context, gmsSuwPackage)
        ) {
            val packageManager = context.packageManager
            if (LOGV) {
                Log.v(
                    TAG,
                    "$GMS_SUW_PACKAGE state = " +
                        packageManager.getApplicationEnabledSetting(gmsSuwPackage),
                )
            }
            return packageManager.getApplicationEnabledSetting(gmsSuwPackage) !=
                COMPONENT_ENABLED_STATE_DISABLED
        }
        return false
    }

    fun hasMicroG(context: Context): Boolean {
        val provisionPackage = context.packageManager.resolveService(
            Intent().apply {
                setComponent(ComponentName(GMS_PACKAGE, MICROG_PROVISION_SERVICE))
            },
            0
        )
        return !hasGMS(context) && provisionPackage != null
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(packageName, GET_ACTIVITIES) }.isSuccess

    fun enableGesturalNavigation(context: Context) {
        if (isManagedProfile(context)) {
            return
        }
        if (!isPackageInstalled(context, NAV_BAR_MODE_GESTURAL_OVERLAY)) {
            return
        }
        val navigationMode =
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.NAVIGATION_MODE, 0)
        if (navigationMode == NAV_BAR_MODE_GESTURAL) {
            return
        }
        val overlayManager =
            IOverlayManager.Stub.asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE))
        runCatching {
            overlayManager.setEnabledExclusiveInCategory(
                NAV_BAR_MODE_GESTURAL_OVERLAY,
                UserHandle.USER_CURRENT,
            )
        }
    }

    private fun restoreWallpaperColorSource(contentResolver: ContentResolver) {
        val overlayPackages =
            Settings.Secure.getString(
                contentResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
            ) ?: return

        runCatching {
                val json = JSONObject(overlayPackages)
                if (json.optString(OVERLAY_COLOR_SOURCE) != COLOR_SOURCE_PRESET) {
                    return
                }
                json.remove(OVERLAY_CATEGORY_SYSTEM_PALETTE)
                json.remove(OVERLAY_CATEGORY_ACCENT_COLOR)
                json.remove(OVERLAY_COLOR_INDEX)
                json.put(OVERLAY_COLOR_SOURCE, COLOR_SOURCE_HOME)
                Settings.Secure.putString(
                    contentResolver,
                    Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                    json.toString(),
                )
            }
            .onFailure { Log.w(TAG, "Could not restore the wallpaper color source", it) }
    }

    fun finishSetupWizard(context: BaseSetupWizardActivity) {
        if (LOGV) {
            Log.v(TAG, "finishSetupWizard")
        }

        if (hasMicroG(context)) {
            startMicroGProvisioning(context)
        }

        val provisioningState = getProvisioningState(context)
        if (LOGV) {
            Log.v(TAG, "finishSetupWizard, provisioningState=$provisioningState")
        }

        if (provisioningState == ProvisioningState.PENDING) {
            Log.e(TAG, "finishSetupWizard, but provisioning pending! Murky waters ahead!")
        }

        val contentResolver = context.contentResolver
        Settings.Global.putInt(contentResolver, Settings.Global.DEVICE_PROVISIONED, 1)
        val userSetupComplete =
            Settings.Secure.getInt(contentResolver, Settings.Secure.USER_SETUP_COMPLETE, 0)
        if (userSetupComplete != 0 && !isManagedProfile(context)) {
            Log.e(
                TAG,
                "finishSetupWizard, but userSetupComplete=$userSetupComplete! " +
                    "This should not happen!",
            )
        }
        restoreWallpaperColorSource(contentResolver)
        Settings.Secure.putInt(contentResolver, Settings.Secure.USER_SETUP_COMPLETE, 1)
        if (hasLeanback(context)) {
            Settings.Secure.putInt(contentResolver, Settings.Secure.TV_USER_SETUP_COMPLETE, 1)
        }
        if (userSetupComplete != 1 && provisioningState == ProvisioningState.COMPLETE) {
            finalizeProvisioning(context)
        }

        handleNavigationOption()
        provisionDefaultUserAppPermissions(context)
        WallpaperManager.getInstance(context).forgetLoadedWallpaper()
        disableHome(context)
        enableStatusBar()
        context.finishAffinity()
        context.nextAction(RESULT_SKIP)
        Log.i(TAG, "Setup complete!")
    }

    fun isSetupWizardComplete(context: Context): Boolean {
        val userSetupComplete =
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.USER_SETUP_COMPLETE, 0)
        return if (userSetupComplete != 0 && !isManagedProfile(context)) {
            true
        } else {
            val enabledSetting =
                context.packageManager.getComponentEnabledSetting(
                    ComponentName(context, SetupWizardActivity::class.java)
                )
            when (enabledSetting) {
                COMPONENT_ENABLED_STATE_DEFAULT,
                COMPONENT_ENABLED_STATE_ENABLED -> false
                else -> true
            }
        }
    }

    fun isBluetoothDisabled(): Boolean =
        SystemProperties.getBoolean("config.disable_bluetooth", false)

    fun isNetworkConnectedToInternetViaEthernet(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val networkCapabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        return networkCapabilities != null &&
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun hasLeanback(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

    fun hasBiometric(context: Context): Boolean {
        val biometricManager = context.getSystemService(BiometricManager::class.java)
        return when (
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        ) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    fun isBootloaderUnlocked(context: Context): Boolean =
        runCatching { context.getSystemService(OemLockManager::class.java)?.isDeviceOemUnlocked }
            .onFailure { e -> Log.e(TAG, "Failed to check if OEM unlock is allowed", e) }
            .getOrNull() ?: true

    fun isOemUnlockAllowed(context: Context): Boolean =
        runCatching { context.getSystemService(OemLockManager::class.java)?.isOemUnlockAllowed }
            .onFailure { e -> Log.e(TAG, "Failed to check if OEM unlock is allowed", e) }
            .getOrNull() ?: true

    fun isFactoryResetProtectionSet(context: Context): Boolean =
        runCatching {
                context.getSystemService(PersistentDataBlockManager::class.java)?.dataBlockSize != 0
            }
            .getOrElse { false }

    /** Disable the Home component, which is presumably SetupWizardActivity at this time. */
    fun disableHome(context: Context) {
        val homeComponent = getHomeComponent(context)
        if (homeComponent != null) {
            setComponentEnabledState(context, homeComponent, COMPONENT_ENABLED_STATE_DISABLED)
        } else {
            Log.w(TAG, "Home component not found. Skipping.")
        }
    }

    private fun getHomeComponent(context: Context): ComponentName? {
        val intent =
            Intent("android.intent.action.MAIN").apply {
                addCategory("android.intent.category.HOME")
                setPackage(context.packageName)
            }
        val comp = intent.resolveActivity(context.packageManager)
        if (LOGV) {
            Log.v(TAG, "resolveActivity for intent=$intent returns $comp")
        }
        return comp
    }

    fun disableComponent(context: Context, cls: Class<*>) {
        setComponentEnabledState(
            context,
            ComponentName(context, cls),
            COMPONENT_ENABLED_STATE_DISABLED,
        )
    }

    fun enableComponent(context: Context, cls: Class<*>) {
        setComponentEnabledState(
            context,
            ComponentName(context, cls),
            COMPONENT_ENABLED_STATE_ENABLED,
        )
    }

    fun setComponentEnabledState(
        context: Context,
        componentName: ComponentName,
        enabledState: Int,
    ) {
        context.packageManager.setComponentEnabledSetting(
            componentName,
            enabledState,
            DONT_KILL_APP,
        )
    }

    private fun handleNavigationOption() {
        val settingsBundle = SetupWizardApp.settingsBundle
        if (settingsBundle.containsKey(NAVIGATION_OPTION_KEY)) {
            val overlayManager =
                IOverlayManager.Stub.asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE))
            val selectedNavMode = settingsBundle.getString(NAVIGATION_OPTION_KEY)
            runCatching {
                overlayManager.setEnabledExclusiveInCategory(
                    selectedNavMode,
                    UserHandle.USER_CURRENT,
                )
            }
        }
    }

    private fun provisionDefaultUserAppPermissions(context: Context) {
        for (packageName in PACKAGE_INSTALLERS) {
            if (LOGV) Log.v(TAG, "Provisioning default permissions for $packageName...")

            runCatching {
                    val uid = context.packageManager.getPackageUid(packageName, 0)
                    val appOpsManager = context.getSystemService(AppOpsManager::class.java)
                    appOpsManager?.setMode(
                        AppOpsManager.OP_REQUEST_INSTALL_PACKAGES,
                        uid,
                        packageName,
                        AppOpsManager.MODE_ALLOWED,
                    )
                }
                .onFailure { e ->
                    when (e) {
                        is PackageManager.NameNotFoundException -> {
                            if (LOGV) Log.v(TAG, "Missing $packageName for appops, skipping")
                        }
                        else -> {
                            Log.e(
                                TAG,
                                "Failed to grant install unknown apps permission to $packageName",
                                e,
                            )
                        }
                    }
                }
            runCatching {
                    val permissionManager = context.getSystemService(PermissionManager::class.java)
                    permissionManager?.grantRuntimePermission(
                        packageName,
                        Manifest.permission.POST_NOTIFICATIONS,
                        context.user,
                    )
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to grant post notifications permission to $packageName", e)
                }
            runCatching {
                    val exemptionManager =
                        context.getSystemService(PowerExemptionManager::class.java)
                    exemptionManager?.addToPermanentAllowList(AURORA_STORE_PACKAGE)
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to add $AURORA_STORE_PACKAGE to power save allowlist", e)
                }
        }
    }

    fun getBuildDateTimestamp(): Long = SystemProperties.getLong(PROP_BUILD_DATE, 0)

    fun simMissing(context: Context): Boolean {
        val tm = context.getSystemService(TelephonyManager::class.java)
        val sm = context.getSystemService(SubscriptionManager::class.java)
        if (tm == null || sm == null) {
            return false
        }
        val hasGsmSim =
            sm.activeSubscriptionInfoList?.any { sub ->
                val simState = tm.getSimState(sub.simSlotIndex)
                if (LOGV) {
                    Log.v(TAG, "getSimState(${sub.subscriptionId}) == $simState")
                }
                simState != -1 &&
                    tm.createForSubscriptionId(sub.subscriptionId).currentPhoneType ==
                        PHONE_TYPE_GSM
            } ?: false
        return !hasGsmSim
    }

    fun enableTalkbackComponent(context: Context) {
        val enabled =
            AccessibilityUtils.getEnabledServicesFromSettings(context, UserHandle.myUserId())
                .isNotEmpty()
        AccessibilityUtils.setAccessibilityServiceState(
            context,
            ComponentName(TALKBACK_PACKAGE, TALKBACK_SERVICE_CLASS),
            !enabled,
        )
    }

    private fun startMicroGProvisioning(context: Context) {
        val enabled = SetupWizardApp.settingsBundle.getBoolean(MICROG_SERVICE_ENABLED)
        arrayOf(GMS_PACKAGE, GMS_FW_PACKAGE, PLAY_STORE_PACKAGE).let { packageList ->
            for (packageId in packageList) {
                context.packageManager.setApplicationEnabledSetting(
                    packageId,
                    if (enabled) {
                        COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        COMPONENT_ENABLED_STATE_DISABLED
                    },
                    0,
                )
            }
        }

        if (!enabled) {
            return
        }

        Intent().apply {
            setClassName(GMS_PACKAGE, MICROG_PROVISION_SERVICE)
            val notifications = SetupWizardApp.settingsBundle.getBoolean(MICROG_PUSH_NOTIFCATIONS)
            putExtra("checkin_enabled", notifications)
            putExtra("gcm_enabled", notifications)
            val location = SetupWizardApp.settingsBundle.getBoolean(MICROG_NETWORK_LOCATION)
            putExtra("wifi_learning", location)
            putExtra("cell_learning", location)
            putExtra("nominatim_enabled", location)
            val provider = LocationProvider.fromValue(
                SetupWizardApp.settingsBundle.getString(MICROG_LOCATION_PROVIDER)
            )
            val useOnlineSource =
                location && provider != LocationProvider.NONE
            putExtra("wifi_ichnaea", useOnlineSource)
            putExtra("cell_ichnaea", useOnlineSource)
            putExtra("online_source_id", provider.value)
        }
        .also { context.startService(it) }
    }
}
