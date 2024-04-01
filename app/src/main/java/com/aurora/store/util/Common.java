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

package com.aurora.store.util;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.aurora.store.data.installer.ShizukuInstaller;
import com.rosan.dhizuku.api.Dhizuku;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rikka.shizuku.Shizuku;
import rikka.sui.Sui;

public class Common {

    public static Intent CUSTOMIZE_TOOL_SERVICE = new Intent("com.saradabar.cpadcustomizetool.data.service.DeviceOwnerService").setPackage("com.saradabar.cpadcustomizetool");

    public static final List<String> list = Arrays.asList("ADB", "デバイスオーナー", "Dhizuku");

    /* データ管理 */
    public static void SET_SETTINGS_FLAG(boolean flag, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("settings_flag", flag).apply();
    }

    public static boolean GET_SETTINGS_FLAG(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("settings_flag", false);
    }

    public static void SET_UPDATE_MODE(Context context, int i) {
        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt("update_mode", i).apply();
    }

    public static int GET_UPDATE_MODE(Context context) {
        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt("update_mode", 0);
    }

    public static boolean isDhizukuActive(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm.isDeviceOwnerApp("com.rosan.dhizuku")) {
            if (Dhizuku.init(context)) {
                return Dhizuku.isPermissionGranted();
            }
        }
        return false;
    }

    public static boolean hasShizukuOrSui(Context context) {
        return PackageUtil.INSTANCE.isInstalled(context, ShizukuInstaller.SHIZUKU_PACKAGE_NAME) || Sui.isSui();
    }

    public static boolean hasShizukuPerm() {
        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
    }

    public static List<String> convertToFilePath(List<File> listFiles) {
        List<String> s = new ArrayList<>();
        for (File file : listFiles) {
            Log.e("TAG", file.getPath());
            s.add(file.getPath());
        }
        return s;
    }
}