/*
 * SPDX-FileCopyrightText: The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.util

import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyManager.ACTION_PROVISION_FINALIZATION
import android.app.admin.DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_MODE
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS
import android.app.admin.DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
import android.app.admin.DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.util.Log
import lineageos.providers.LineageSettings
import lineageos.providers.LineageSettings.Global.GARLIC_LEVEL
import org.lineageos.setupwizard.LOGV

object ManagedProvisioningUtils {

    private const val TAG = "ManagedProvisioningUtils"

    private const val GARLIC_LEVEL_STANDARD = 0
    private const val GARLIC_LEVEL_SAFER = 1
    private const val GARLIC_LEVEL_SAFEST = 2
    private const val GARLIC_LEVEL_DEFAULT = GARLIC_LEVEL_STANDARD

    private const val BELLIS_PACKAGE = "org.calyxos.bellis"
    private const val BELLIS_DEVICE_ADMIN_RECEIVER_CLASS =
        "org.calyxos.bellis.BasicDeviceAdminReceiver"

    enum class ProvisioningState {
        UNSUPPORTED,
        PENDING,
        COMPLETE,
    }

    fun getProvisioningState(context: Context): ProvisioningState {
        // Only system user can provision the device
        if (!SetupWizardUtils.isOwner()) {
            return ProvisioningState.UNSUPPORTED
        }

        val provisioningMode = getProvisioningMode(context) ?: return ProvisioningState.UNSUPPORTED
        val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
        return when (provisioningMode) {
            PROVISIONING_MODE_MANAGED_PROFILE -> {
                // In managed profile provisioning mode, having no policy-managed profiles yet
                // return PENDING; otherwise, return COMPLETE.
                devicePolicyManager
                    .getPolicyManagedProfiles(context.user)
                    .takeIf { it.isNotEmpty() }
                    ?.let { ProvisioningState.COMPLETE } ?: ProvisioningState.PENDING
            }
            PROVISIONING_MODE_FULLY_MANAGED_DEVICE -> {
                // In fully-managed mode, if Bellis is not the device owner yet, return PENDING;
                // otherwise, return COMPLETE.
                devicePolicyManager
                    .takeIf { it.isDeviceOwnerAppOnAnyUser(BELLIS_PACKAGE) }
                    ?.let { ProvisioningState.COMPLETE } ?: ProvisioningState.PENDING
            }
            else -> ProvisioningState.UNSUPPORTED
        }
    }

    fun startProvisioning(context: Context) {
        val provisioningMode =
            getProvisioningMode(context)
                ?: throw UnsupportedOperationException("provisioningMode is null")
        val persistableBundle =
            PersistableBundle().apply {
                putInt(EXTRA_PROVISIONING_MODE, provisioningMode)
                putInt(GARLIC_LEVEL, getGarlicLevel(context))
            }

        Intent(ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE)
            .apply {
                putExtra(
                    EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                    ComponentName(BELLIS_PACKAGE, BELLIS_DEVICE_ADMIN_RECEIVER_CLASS),
                )
                putExtra(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, persistableBundle)
                putExtra(EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS, true)
                if (provisioningMode == PROVISIONING_MODE_FULLY_MANAGED_DEVICE) {
                    putExtra(EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED, true)
                }
            }
            .let { context.startActivityForResult("", it, 0, null) }
    }

    fun finalizeProvisioning(context: Context) {
        if (LOGV) {
            Log.v(TAG, "finalizeProvisioning")
        }
        context.startActivity(
            Intent(ACTION_PROVISION_FINALIZATION).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        )
    }

    private fun getProvisioningMode(context: Context): Int? =
        when (getGarlicLevel(context)) {
            GARLIC_LEVEL_STANDARD -> null
            else -> DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE
        }

    private fun getGarlicLevel(context: Context): Int =
        LineageSettings.Global.getInt(context.contentResolver, GARLIC_LEVEL, GARLIC_LEVEL_DEFAULT)
}
