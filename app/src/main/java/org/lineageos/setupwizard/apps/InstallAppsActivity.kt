/*
 * SPDX-FileCopyrightText: The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.apps

import android.content.Intent
import org.lineageos.setupwizard.base.SubBaseActivity

class InstallAppsActivity : SubBaseActivity() {

    override fun onStartSubactivity() {
        startSubactivity(Intent("org.calyxos.lupin.INSTALL"))
    }
}
