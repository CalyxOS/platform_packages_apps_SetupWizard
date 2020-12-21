/*
 * SPDX-FileCopyrightText: The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.system

import android.os.Build.IS_DEBUGGABLE
import android.os.Bundle
import android.os.PowerManager
import com.google.android.setupcompat.util.ResultCodes.RESULT_SKIP
import org.lineageos.setupwizard.R
import org.lineageos.setupwizard.base.BaseSetupWizardActivity
import org.lineageos.setupwizard.util.SetupWizardUtils.isBootloaderUnlocked

class BootloaderWarningActivity : BaseSetupWizardActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isBootloaderUnlocked(this) || IS_DEBUGGABLE) {
            finishAction(RESULT_SKIP)
            return
        }

        setDescriptionText(getString(R.string.bootloader_warning_summary))
        setNextText(R.string.reboot)
        setNextAllowed(true)
    }

    override fun onNextPressed() {
        getSystemService(PowerManager::class.java)?.reboot("bootloader")
    }

    override val layoutResId = R.layout.warning_activity

    override val titleResId = R.string.bootloader_warning_title

    override val iconResId = R.drawable.ic_warning

    override val showSkipButton = true
}
