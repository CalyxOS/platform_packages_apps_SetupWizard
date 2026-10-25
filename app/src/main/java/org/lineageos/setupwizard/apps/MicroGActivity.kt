/*
 * SPDX-FileCopyrightText: The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.apps

import android.content.Intent
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import android.os.Bundle
import com.google.android.setupcompat.util.ResultCodes.RESULT_SKIP
import com.google.android.setupdesign.items.SwitchItem
import org.lineageos.setupwizard.R
import org.lineageos.setupwizard.SetupWizardApp
import org.lineageos.setupwizard.base.BaseSetupWizardActivity
import org.lineageos.setupwizard.privacy.LocationProvider
import org.lineageos.setupwizard.util.SetupWizardUtils
import org.lineageos.setupwizard.util.SetupWizardUtils.GMS_PACKAGE
import org.lineageos.setupwizard.util.SetupWizardUtils.MICROG_LOCATION_PROVIDER
import org.lineageos.setupwizard.util.SetupWizardUtils.MICROG_NETWORK_LOCATION

class MicroGActivity : BaseSetupWizardActivity() {

    private val enableSwitch by lazy {
        itemAdapter.findItemById(R.id.gms_compatibility) as SwitchItem
    }
    private val notificationSwitch by lazy {
        itemAdapter.findItemById(R.id.push_notifications) as SwitchItem
    }

    private val locationProvider by lazy {
        LocationProvider.fromValue(
            SetupWizardApp.settingsBundle.getString(MICROG_LOCATION_PROVIDER)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SetupWizardUtils.hasMicroG(this)) {
            finishAction(RESULT_SKIP)
            return
        }

        setDescriptionText(getString(R.string.microg_description2))

        enableSwitch.apply {
            summary =
                getString(
                    R.string.microg_summary,
                    if (SetupWizardUtils.isManagedProfile(this@MicroGActivity)) {
                        getString(R.string.managed_profile)
                    } else {
                        getString(R.string.personal_profile)
                    },
                )
            setOnCheckedChangeListener { _, isChecked -> setInstallPreferences(isChecked) }
        }

        setInstallPreferences(enableSwitch.isChecked)
    }

    override fun onNextPressed() {
        arrayOf(GMS_PACKAGE, GMS_FW_PACKAGE, PLAY_STORE_PACKAGE).let { packageList ->
            for (packageId in packageList) {
                packageManager.setApplicationEnabledSetting(
                    packageId,
                    if (enableSwitch.isChecked) {
                        COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        COMPONENT_ENABLED_STATE_DISABLED
                    },
                    0,
                )
            }
        }

        if (enableSwitch.isChecked) {
            Intent()
                .apply {
                    setClassName(GMS_PACKAGE, GMS_PROVISION_PACKAGE)
                    putExtra("checkin_enabled", notificationSwitch.isChecked)
                    putExtra("gcm_enabled", notificationSwitch.isChecked)
                    val locationSwitch =
                        SetupWizardApp.settingsBundle.getBoolean(MICROG_NETWORK_LOCATION)
                    putExtra("wifi_learning", locationSwitch)
                    putExtra("cell_learning", locationSwitch)
                    putExtra("nominatim_enabled", locationSwitch)
                    val useOnlineSource =
                        locationSwitch && locationProvider != LocationProvider.NONE
                    putExtra("wifi_ichnaea", useOnlineSource)
                    putExtra("cell_ichnaea", useOnlineSource)
                    putExtra("online_source_id", locationProvider.value)
                }
                .also { startService(it) }
        }

        super.onNextPressed()
    }

    override val layoutResId = R.layout.setup_microg

    override val titleResId = R.string.microg_title

    override val iconResId = R.drawable.microg_logo

    private fun setInstallPreferences(enable: Boolean) {
        notificationSwitch.isEnabled = enable
        notificationSwitch.isChecked = enable
    }

    companion object {
        private const val GMS_FW_PACKAGE = "com.google.android.gsf"
        private const val PLAY_STORE_PACKAGE = "com.android.vending"
        private const val GMS_PROVISION_PACKAGE = "org.microg.gms.provision.ProvisionService"
    }
}
