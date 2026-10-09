/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.privacy

import android.location.LocationManager
import android.os.Bundle
import android.os.Process
import android.os.UserManager
import android.provider.Settings
import com.google.android.setupdesign.items.RadioButtonItem
import com.google.android.setupdesign.items.SwitchItem
import org.lineageos.setupwizard.R
import org.lineageos.setupwizard.SetupWizardApp
import org.lineageos.setupwizard.base.BaseSetupWizardActivity
import org.lineageos.setupwizard.util.SetupWizardUtils

class LocationSettingsActivity : BaseSetupWizardActivity() {

    private val locationAccess by lazy {
        itemAdapter.findItemById(R.id.location_item) as SwitchItem
    }
    private val locationAgpsAccess by lazy {
        itemAdapter.findItemById(R.id.location_agps_item) as SwitchItem
    }
    private val locationNetworkAccess by lazy {
        itemAdapter.findItemById(R.id.location_network_item) as SwitchItem
    }

    private val locationManager by lazy { getSystemService(LocationManager::class.java) }
    private val userManager by lazy { getSystemService(UserManager::class.java) }

    private var selectedProvider = LocationProvider.POSITION
    private var isUpdatingProviders = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDescriptionText(getString(R.string.location_summary))

        locationAgpsAccess.isVisible = userManager.isMainUser

        itemAdapter.setOnItemSelectedListener { item ->
            if (item is SwitchItem) {
                item.isChecked = !item.isChecked
            }
        }

        LocationProvider.entries.forEach { provider ->
            locationProviderItem(provider).setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && !isUpdatingProviders) {
                    setLocationProvider(provider)
                }
            }
        }

        setLocationProvider(LocationProvider.POSITION)
    }

    override fun onResume() {
        super.onResume()
        var checked = locationManager.isLocationEnabled
        if (userManager.isManagedProfile) {
            checked =
                checked and userManager.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION)
        }
        locationAccess.isChecked = checked
    }

    override fun onNextPressed() {
        locationManager.setLocationEnabledForUser(locationAccess.isChecked, Process.myUserHandle())
        if (userManager.isManagedProfile) {
            userManager.setUserRestriction(
                UserManager.DISALLOW_SHARE_LOCATION,
                !locationAccess.isChecked,
            )
        }
        Settings.Global.putInt(
            contentResolver,
            Settings.Global.ASSISTED_GPS_ENABLED,
            if (locationAgpsAccess.isChecked) 1 else 0,
        )
        SetupWizardApp.settingsBundle.apply {
            putBoolean(SetupWizardUtils.MICROG_NETWORK_LOCATION, locationNetworkAccess.isChecked)
            putString(SetupWizardUtils.MICROG_LOCATION_PROVIDER, selectedProvider.value)
        }
        super.onNextPressed()
    }

    private fun locationProviderItem(provider: LocationProvider) =
        itemAdapter.findItemById(provider.itemId) as RadioButtonItem

    private fun setLocationProvider(selection: LocationProvider) {
        LocationProvider.entries.forEach { provider ->
            locationProviderItem(provider).isVisible = locationNetworkAccess.isChecked
        }

        if (isUpdatingProviders) return
        isUpdatingProviders = true

        selectedProvider = selection
        LocationProvider.entries.forEach { provider ->
            val item = locationProviderItem(provider)
            val shouldBeChecked = provider == selection
            if (item.isChecked != shouldBeChecked) {
                item.isChecked = shouldBeChecked
            }
        }

        isUpdatingProviders = false
    }

    override val layoutResId = R.layout.location_settings

    override val titleResId = R.string.setup_location

    override val iconResId = R.drawable.ic_location
}
