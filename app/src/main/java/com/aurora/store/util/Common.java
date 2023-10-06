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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class Common {

    public static Intent BIND_CUSTOMIZE_TOOL = new Intent("com.saradabar.cpadcustomizetool.data.service.DeviceOwnerService").setPackage("com.saradabar.cpadcustomizetool");
    public static String DOWNLOAD_FILE_URL;

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
}