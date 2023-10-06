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

package com.aurora.store.view.ui.preferences;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.aurora.store.R;
import com.aurora.store.util.Common;
import com.aurora.store.view.epoxy.views.UpdateModeView;
import com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService;

import java.util.ArrayList;
import java.util.List;

public class OtherPreference extends PreferenceFragmentCompat {

    IDeviceOwnerService mDeviceOwnerService;
    private DevicePolicyManager mDevicePolicyManager;

    Preference preferenceDisableOwner,
            preferenceNowSetOwnerApp,
            preferenceUpdateMode;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.pref_cpad_other_title);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_cpad_other, rootKey);

        mDevicePolicyManager = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

        preferenceDisableOwner = findPreference("pref_other_owner_disable");
        preferenceNowSetOwnerApp = findPreference("pref_other_now_owner_package");
        preferenceUpdateMode = findPreference("pref_other_update_mode");

        preferenceDisableOwner.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setMessage(getString(R.string.dialog_cpad_clear_device_owner))
                    .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog, which) -> {
                        mDevicePolicyManager.clearDeviceOwnerApp(requireActivity().getPackageName());
                        requireActivity().finishAffinity();
                    })
                    .setNegativeButton(R.string.dialog_cpad_common_no, null)
                    .show();
            return false;
        });

        preferenceUpdateMode.setOnPreferenceClickListener(preference -> {
            View v = requireActivity().getLayoutInflater().inflate(R.layout.layout_cpad_update_list, null);
            List<String> list = new ArrayList<>();
            list.add("ADB");
            list.add("デバイスオーナー");
            list.add("CPad Customize Tool");
            List<UpdateModeView.AppData> dataList = new ArrayList<>();
            int i = 0;
            for (String str : list) {
                UpdateModeView.AppData data = new UpdateModeView.AppData();
                data.label = str;
                data.updateMode = i;
                dataList.add(data);
                i++;
            }
            ListView listView = v.findViewById(R.id.list_cpad_update);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            listView.setAdapter(new UpdateModeView.AppListAdapter(requireActivity(), dataList));
            listView.setOnItemClickListener((parent, mView, position, id) -> {
                switch (position) {
                    case 0 -> {
                        Common.SET_UPDATE_MODE(requireActivity(), (int) id);
                        listView.invalidateViews();
                    }
                    case 1 -> {
                        if (((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(requireActivity().getPackageName())) {
                            Common.SET_UPDATE_MODE(requireActivity(), (int) id);
                            listView.invalidateViews();
                        } else {
                            new AlertDialog.Builder(requireActivity())
                                    .setMessage(getString(R.string.dialog_cpad_error_not_work_mode))
                                    .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                    }
                    case 2 -> {
                        if (bindDeviceOwnerService()) {
                            Common.SET_UPDATE_MODE(requireActivity(), 2);
                            listView.invalidateViews();
                        } else {
                            new AlertDialog.Builder(requireActivity())
                                    .setMessage(getString(R.string.dialog_cpad_error_not_work_mode))
                                    .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                    }
                }
            });
            new AlertDialog.Builder(requireActivity())
                    .setView(v)
                    .setTitle(getString(R.string.dialog_cpad_title_select_mode))
                    .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        if (getNowOwnerPackage() != null) {
            preferenceNowSetOwnerApp.setSummary("デバイスオーナーは" + getNowOwnerPackage() + "に設定されています");
        } else preferenceNowSetOwnerApp.setSummary("デバイスオーナーはデバイスに設定されていません");

        if (!mDevicePolicyManager.isDeviceOwnerApp(requireActivity().getPackageName())) {
            preferenceDisableOwner.setEnabled(false);
            preferenceDisableOwner.setSelectable(false);
        }
    }

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDeviceOwnerService = IDeviceOwnerService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mDeviceOwnerService = null;
        }
    };

    public boolean bindDeviceOwnerService() {
        try {
            return requireActivity().bindService(Common.BIND_CUSTOMIZE_TOOL, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ignored) {
            return false;
        }
    }


    private String getNowOwnerPackage() {
        for (ApplicationInfo app : requireActivity().getPackageManager().getInstalledApplications(0)) {
            /* ユーザーアプリか確認 */
            if (app.sourceDir.startsWith("/data/app/")) {
                if (mDevicePolicyManager.isDeviceOwnerApp(app.packageName)) {
                    return app.loadLabel(requireActivity().getPackageManager()).toString();
                }
            }
        }
        return null;
    }
}