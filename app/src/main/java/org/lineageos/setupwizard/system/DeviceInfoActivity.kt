/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.system

import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.telephony.euicc.EuiccManager
import android.util.Log
import com.google.android.setupdesign.items.ItemGroup
import com.google.android.setupdesign.items.NonActionableItem
import com.google.android.setupdesign.items.RecyclerItemAdapter
import org.lineageos.setupwizard.R
import org.lineageos.setupwizard.base.BaseSetupWizardActivity
import org.lineageos.setupwizard.util.SetupWizardUtils

class DeviceInfoActivity : BaseSetupWizardActivity() {

    private val bootloaderUnlocked: Boolean
        get() = SetupWizardUtils.isBootloaderUnlocked(this)

    private val oemUnlockAllowed: Boolean
        get() = SetupWizardUtils.isOemUnlockAllowed(this)

    private val resetProtectionSet: Boolean
        get() = SetupWizardUtils.isFactoryResetProtectionSet(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setNextText(R.string.ok)

        val itemGroup = ItemGroup()

        for ((label, value) in bootloaderIdentifiers()) {
            NonActionableItem()
                .apply {
                    title = label
                    summary = value
                }
                .also { itemGroup.addChild(it) }
        }

        for ((label, value) in deviceIdentifiers()) {
            itemGroup.addChild(DeviceInfoItem(label, value))
        }

        val adapter = RecyclerItemAdapter(itemGroup)
        adapter.setOnItemSelectedListener { (it as? DeviceInfoItem)?.toggle() }
        itemAdapter = adapter
    }

    private fun bootloaderIdentifiers(): List<Pair<String, String>> {
        val identifiers = mutableListOf<Pair<String, String>>()

        val bootloader = getString(if (bootloaderUnlocked) R.string.unlocked else R.string.locked)
        identifiers += getString(R.string.device_info_bootloader) to bootloader

        val oemunlock =
            getString(
                when {
                    bootloaderUnlocked -> R.string.notapplicable
                    oemUnlockAllowed -> R.string.allowed
                    else -> R.string.notallowed
                }
            )
        identifiers += getString(R.string.device_info_oemunlock) to oemunlock

        val frp = getString(if (resetProtectionSet) R.string.set else R.string.unset)
        identifiers += getString(R.string.device_info_frp) to frp

        if (identifiers.isEmpty()) {
            Log.w(TAG, "No bootloader identifiers to show")
        }
        return identifiers
    }

    private fun deviceIdentifiers(): List<Pair<String, String>> {
        val identifiers = mutableListOf<Pair<String, String>>()

        val telephonyManager = getSystemService(TelephonyManager::class.java)
        if (telephonyManager != null) {
            val slotCount = telephonyManager.activeModemCount
            for (slot in 0 until slotCount) {
                val imei = runCatching { telephonyManager.getImei(slot) }.getOrNull()
                if (!imei.isNullOrEmpty()) {
                    val label =
                        if (slotCount > 1) {
                            getString(R.string.device_info_imei_slot, slot + 1)
                        } else {
                            getString(R.string.device_info_imei)
                        }
                    identifiers += label to imei
                }
            }
        }

        val eid = runCatching { getSystemService(EuiccManager::class.java)?.eid }.getOrNull()
        if (!eid.isNullOrEmpty()) {
            identifiers += getString(R.string.device_info_eid) to eid
        }

        val serial = runCatching { Build.getSerial() }.getOrNull()
        if (!serial.isNullOrEmpty() && serial != Build.UNKNOWN) {
            identifiers += getString(R.string.device_info_serial) to serial
        }

        if (identifiers.isEmpty()) {
            Log.w(TAG, "No device identifiers to show")
        }
        return identifiers
    }

    override fun onNextPressed() {
        finish()
    }

    override fun onSkipPressed() {
        finish()
    }

    override val layoutResId = R.layout.device_info_activity

    override val titleResId = R.string.device_information

    override val iconResId = R.drawable.ic_info

    companion object {
        private const val TAG = "DeviceInfoActivity"
    }
}
