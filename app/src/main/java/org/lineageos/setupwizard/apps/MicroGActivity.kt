/*
 * SPDX-FileCopyrightText: The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.apps

import android.os.Bundle
import com.google.android.setupcompat.util.ResultCodes.RESULT_SKIP
import com.google.android.setupdesign.items.RadioButtonItem
import com.google.android.setupdesign.items.SwitchItem
import org.lineageos.setupwizard.R
import org.lineageos.setupwizard.SetupWizardApp
import org.lineageos.setupwizard.base.BaseSetupWizardActivity
import org.lineageos.setupwizard.util.SetupWizardUtils
import org.lineageos.setupwizard.util.SetupWizardUtils.MICROG_LOCATION_PROVIDER
import org.lineageos.setupwizard.util.SetupWizardUtils.MICROG_NETWORK_LOCATION
import org.lineageos.setupwizard.util.SetupWizardUtils.MICROG_PUSH_NOTIFCATIONS
import org.lineageos.setupwizard.util.SetupWizardUtils.MICROG_SERVICE_ENABLED

class MicroGActivity : BaseSetupWizardActivity() {

    private val enableSwitch by lazy {
        itemAdapter.findItemById(R.id.gms_compatibility) as SwitchItem
    }
    private val notificationSwitch by lazy {
        itemAdapter.findItemById(R.id.push_notifications) as SwitchItem
    }
    private val locationSwitch by lazy {
        itemAdapter.findItemById(R.id.network_location) as SwitchItem
    }

    private enum class LocationProvider(
        val value: String?,
        val itemId: Int,
        val summaryResId: Int,
    ) {
        POSITION(
            "position",
            R.id.online_source_position,
            R.string.microg_online_source_positon_summary,
        ),
        BEACONDB(
            "beacondb",
            R.id.online_source_beacondb,
            R.string.microg_online_source_beacondb_summary,
        ),
        NONE(null, R.id.online_source_none, R.string.microg_online_source_none_summary);

        companion object {
            fun fromValue(value: String?): LocationProvider =
                entries.firstOrNull { it.value == value } ?: NONE
        }
    }

    private var locationProvider = LocationProvider.POSITION
    private var isUpdatingProviders = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasMicroG(this)) {
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

        LocationProvider.entries.forEach { provider ->
            locationProviderItem(provider).setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && !isUpdatingProviders) {
                    setLocationProvider(provider)
                }
            }
        }

        setLocationProvider(LocationProvider.POSITION)
    }

    override fun onNextPressed() {
        SetupWizardApp.settingsBundle.apply {
            putBoolean(MICROG_SERVICE_ENABLED, enableSwitch.isChecked)
            putBoolean(MICROG_PUSH_NOTIFCATIONS, notificationSwitch.isChecked)
            putBoolean(MICROG_NETWORK_LOCATION, notificationSwitch.isChecked)
            putString(MICROG_LOCATION_PROVIDER, locationProvider.value)
        }
        super.onNextPressed()
    }

    override val layoutResId = R.layout.setup_microg

    override val titleResId = R.string.microg_title

    override val iconResId = R.drawable.microg_logo

    private fun setInstallPreferences(enable: Boolean) {
        notificationSwitch.isEnabled = enable
        notificationSwitch.isChecked = enable
        locationSwitch.isEnabled = enable
        locationSwitch.isChecked = enable
        LocationProvider.entries.forEach { provider ->
            locationProviderItem(provider).isVisible = locationSwitch.isChecked
        }
    }

    private fun locationProviderItem(provider: LocationProvider) =
        itemAdapter.findItemById(provider.itemId) as RadioButtonItem

    private fun setLocationProvider(selection: LocationProvider) {
        if (isUpdatingProviders) return
        isUpdatingProviders = true

        locationProvider = selection
        LocationProvider.entries.forEach { provider ->
            val item = locationProviderItem(provider)
            val shouldBeChecked = provider == selection
            if (item.isChecked != shouldBeChecked) {
                item.isChecked = shouldBeChecked
            }
        }

        isUpdatingProviders = false
    }
}
