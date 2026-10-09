/*
 * SPDX-FileCopyrightText: The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.privacy

import org.lineageos.setupwizard.R

enum class LocationProvider(
    /** Value stored in the settings bundle and passed to microG. */
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
