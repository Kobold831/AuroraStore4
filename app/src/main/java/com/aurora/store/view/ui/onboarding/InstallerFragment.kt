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

package com.aurora.store.view.ui.onboarding

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import com.aurora.extensions.showDialog
import com.aurora.store.R
import com.aurora.store.data.model.Installer
import com.aurora.store.databinding.FragmentOnboardingInstallerBinding
import com.aurora.store.util.Common
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID
import com.aurora.store.util.save
import com.aurora.store.view.epoxy.views.preference.InstallerViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.google.gson.reflect.TypeToken
import com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService
import java.nio.charset.StandardCharsets


class InstallerFragment : BaseFragment(R.layout.fragment_onboarding_installer) {

    var mDeviceOwnerService: IDeviceOwnerService? = null

    private var _binding: FragmentOnboardingInstallerBinding? = null
    private val binding get() = _binding!!

    private var installerId: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingInstallerBinding.bind(view)

        installerId = Preferences.getInteger(requireContext(), PREFERENCE_INSTALLER_ID)

        // RecyclerView
        binding.epoxyRecycler.withModels {
            setFilterDuplicates(true)
            loadInstallersFromAssets().forEach {
                add(
                    InstallerViewModel_()
                        .id(it.id)
                        .installer(it)
                        .markChecked(installerId == it.id)
                        .click { _ ->
                            save(it.id)
                            requestModelBuild()
                        }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun save(installerId: Int) {
        when (installerId) {
            0 -> {
                if (!hasDeviceOwnerService()) {
                    if (mDeviceOwnerService!!.isDeviceOwnerApp) {
                        this.installerId = installerId
                        save(PREFERENCE_INSTALLER_ID, installerId)
                    } else {
                        showDialog(
                            R.string.action_installations,
                            R.string.dialog_cpad_installer_error_failure_bind
                        )
                    }
                } else {
                    showDialog(
                        R.string.action_installations,
                        R.string.dialog_cpad_installer_error_failure_bind
                    )
                }
            }
            1 -> {
            }
        }
    }

    private fun loadInstallersFromAssets(): List<Installer> {
        val inputStream = requireContext().assets.open("installers.json")
        val bytes = ByteArray(inputStream.available())
        inputStream.read(bytes)
        inputStream.close()

        val json = String(bytes, StandardCharsets.UTF_8)
        return gson.fromJson<MutableList<Installer>?>(
            json,
            object : TypeToken<MutableList<Installer?>?>() {}.type
        )
    }

    private var mDeviceOwnerServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            mDeviceOwnerService = IDeviceOwnerService.Stub.asInterface(iBinder)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
        }
    }

    private fun hasDeviceOwnerService(): Boolean {
        return try {
            requireContext().bindService(
                Common.CUSTOMIZE_TOOL_SERVICE,
                mDeviceOwnerServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        } catch (ignored: Exception) {
            false
        }
    }
}