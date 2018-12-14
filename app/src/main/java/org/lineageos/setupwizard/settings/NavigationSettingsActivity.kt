/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.settings

import android.os.Bundle
import android.view.View
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import org.lineageos.setupwizard.NAVIGATION_OPTION_KEY
import org.lineageos.setupwizard.R
import org.lineageos.setupwizard.SetupWizardApp
import org.lineageos.setupwizard.base.BaseSetupWizardActivity
import org.lineageos.setupwizard.util.SetupWizardUtils
import org.lineageos.setupwizard.util.updateCheckedIcons

class NavigationSettingsActivity : BaseSetupWizardActivity() {

    private var selection: String = NAV_BAR_MODE_GESTURAL_OVERLAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setDescriptionText(getString(R.string.navigation_summary))

        val modeGroup = findViewById<MaterialButtonToggleGroup>(R.id.navigation_mode_group)
        val gestureButton = findViewById<MaterialButton>(R.id.mode_gesture)
        val swKeysButton = findViewById<MaterialButton>(R.id.mode_sw_keys)
        val navigationIllustration = findViewById<LottieAnimationView>(R.id.navigation_illustration)

        modeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }
            when (checkedId) {
                R.id.mode_gesture -> {
                    selection = NAV_BAR_MODE_GESTURAL_OVERLAY
                    navigationIllustration.setAnimation(R.raw.lottie_system_nav_fully_gestural)
                }

                R.id.mode_sw_keys -> {
                    selection = NAV_BAR_MODE_3BUTTON_OVERLAY
                    navigationIllustration.setAnimation(R.raw.lottie_system_nav_3_button)
                }
            }
            modeGroup.updateCheckedIcons(R.drawable.ic_check)
            navigationIllustration.playAnimation()
        }

        var available = 3
        if (!SetupWizardUtils.isPackageInstalled(this, NAV_BAR_MODE_GESTURAL_OVERLAY)) {
            gestureButton.visibility = View.GONE
            modeGroup.check(R.id.mode_sw_keys)
            available--
        }

        if (!SetupWizardUtils.isPackageInstalled(this, NAV_BAR_MODE_3BUTTON_OVERLAY)) {
            swKeysButton.visibility = View.GONE
            available--
        }

        if (available <= 1) {
            SetupWizardApp.settingsBundle.putString(
                NAVIGATION_OPTION_KEY,
                NAV_BAR_MODE_3BUTTON_OVERLAY,
            )
            finishAction(RESULT_OK)
        }

        modeGroup.updateCheckedIcons(R.drawable.ic_check)
    }

    override fun onNextPressed() {
        SetupWizardApp.settingsBundle.putString(NAVIGATION_OPTION_KEY, selection)
        super.onNextPressed()
    }

    override val layoutResId = R.layout.setup_navigation

    override val titleResId = R.string.setup_navigation

    override val iconResId = R.drawable.ic_navigation
}
