/*
 * SPDX-FileCopyrightText: The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.apps

import android.os.Bundle
import com.google.android.setupcompat.util.ResultCodes.RESULT_SKIP
import com.google.android.setupdesign.items.SwitchItem
import org.lineageos.setupwizard.R
import org.lineageos.setupwizard.SetupWizardApp
import org.lineageos.setupwizard.base.BaseSetupWizardActivity
import org.lineageos.setupwizard.util.SetupWizardUtils
import org.lineageos.setupwizard.util.SetupWizardUtils.MICROG_PUSH_NOTIFCATIONS
import org.lineageos.setupwizard.util.SetupWizardUtils.MICROG_SERVICE_ENABLED

class MicroGActivity : BaseSetupWizardActivity() {

    private val enableSwitch by lazy {
        itemAdapter.findItemById(R.id.gms_compatibility) as SwitchItem
    }
    private val notificationSwitch by lazy {
        itemAdapter.findItemById(R.id.push_notifications) as SwitchItem
    }

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
    }

    override fun onNextPressed() {
        SetupWizardApp.settingsBundle.apply {
            putBoolean(MICROG_SERVICE_ENABLED, enableSwitch.isChecked)
            putBoolean(MICROG_PUSH_NOTIFCATIONS, notificationSwitch.isChecked)
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
}
