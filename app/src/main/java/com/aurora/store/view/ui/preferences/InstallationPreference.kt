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
import com.aurora.extensions.runOnUiThread
import com.aurora.extensions.showDialog
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.Log
import com.aurora.store.util.Preferences
import com.aurora.store.util.save
import com.aurora.store.view.custom.preference.AuroraListPreference
import com.aurora.store.view.custom.preference.ListPreferenceMaterialDialogFragmentCompat
import com.aurora.store.view.custom.preference.ListPreferenceMaterialDialogFragmentCompat.Companion.PREFERENCE_DIALOG_FRAGMENT_TAG
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.AndroidEntryPoint
import rikka.shizuku.Shizuku
import rikka.sui.Sui

@AndroidEntryPoint
class InstallationPreference : PreferenceFragmentCompat() {

    private var shizukuAlive = Sui.isSui()
    private val shizukuAliveListener = Shizuku.OnBinderReceivedListener {
        Log.d("ShizukuInstaller Alive!")
        shizukuAlive = true
    }
    private val shizukuDeadListener = Shizuku.OnBinderDeadListener {
        Log.d("ShizukuInstaller Dead!")
        shizukuAlive = false
    }
    private val shizukuResultListener =
        Shizuku.OnRequestPermissionResultListener { _: Int, result: Int ->
            if (result == PackageManager.PERMISSION_GRANTED) {
                save(Preferences.PREFERENCE_INSTALLER_ID, 5)
                activity?.recreate()
            } else {
                showDialog(
                    R.string.action_installations,
                    R.string.installer_shizuku_unavailable
                )
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_installation, rootKey)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ListPreference) {
            val dialogFragment = ListPreferenceMaterialDialogFragmentCompat.newInstance(preference.getKey())
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
                    if (selectedId == 1) {
                        showDialog(
                            R.string.action_installations,
                            R.string.dialog_cpad_error_not_work_mode
                        )
                        false
                    } else if (selectedId == 2) {
                        if (checkRootAvailability()) {
                            save(Preferences.PREFERENCE_INSTALLER_ID, selectedId)
                            true
                        } else {
                            showDialog(
                                R.string.action_installations,
                                R.string.installer_root_unavailable
                            )
                            false
                        }
                    } else if (selectedId == 3) {
                        showDialog(
                            R.string.action_installations,
                            R.string.installer_service_unavailable
                        )
                        false
                    } else if (selectedId == 4) {
                        showDialog(
                            R.string.action_installations,
                            R.string.installer_am_unavailable
                        )
                        false
                    } else if (selectedId == 5) {
                        showDialog(
                            R.string.action_installations,
                            R.string.installer_shizuku_unavailable
                        )
                        false
                    } else {
                        save(Preferences.PREFERENCE_INSTALLER_ID, selectedId)
                        true
                    }
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun checkRootAvailability(): Boolean {
        return Shell.getShell().isRoot
    }
}
