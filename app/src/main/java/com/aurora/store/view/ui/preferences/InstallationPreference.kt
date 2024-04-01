/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.view.ui.preferences

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.runOnUiThread
import com.aurora.extensions.showDialog
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.Log
import com.aurora.store.util.Preferences
import com.aurora.store.util.save
import com.aurora.store.view.custom.preference.AuroraListPreference
import com.aurora.store.view.custom.preference.ListPreferenceMaterialDialogFragmentCompat
import com.aurora.store.view.custom.preference.ListPreferenceMaterialDialogFragmentCompat.Companion.PREFERENCE_DIALOG_FRAGMENT_TAG
import dagger.hilt.android.AndroidEntryPoint
import rikka.shizuku.Shizuku
import rikka.sui.Sui

@AndroidEntryPoint
class InstallationPreference : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_installation, rootKey)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ListPreference) {
            val dialogFragment =
                ListPreferenceMaterialDialogFragmentCompat.newInstance(preference.getKey())
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, PREFERENCE_DIALOG_FRAGMENT_TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.title_installation)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        val abandonPreference: Preference? =
            findPreference(Preferences.INSTALLATION_ABANDON_SESSION)

        abandonPreference?.let {
            it.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    CommonUtil.cleanupInstallationSessions(requireContext())
                    runOnUiThread {
                        requireContext().toast(R.string.toast_abandon_sessions)
                    }
                    false
                }
        }

        val installerPreference: AuroraListPreference? =
            findPreference(Preferences.PREFERENCE_INSTALLER_ID)

        installerPreference?.let {
            it.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    val selectedId = Integer.parseInt(newValue as String)
                    when (selectedId) {
                        /* セッションインストーラー（デバイスオーナー） */
                        0 -> {
                            //セーブさせない
                            false
                        }
                        /* Shizukuインストーラー */
                        1 -> {
                            //セーブさせない
                            false
                        }
                        /* Dhizukuインストーラー */
                        2 -> {
                            //セーブさせない
                            false
                        }

                        else -> {
                            false
                        }
                    }
                }
        }
    }
}