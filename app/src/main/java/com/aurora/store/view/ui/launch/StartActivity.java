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

package com.aurora.store.view.ui.launch;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.aurora.Constants;
import com.aurora.extensions.ToastKt;
import com.aurora.store.MainActivity;
import com.aurora.store.R;
import com.aurora.store.data.connection.AsyncFileDownload;
import com.aurora.store.data.connection.Checker;
import com.aurora.store.data.connection.Updater;
import com.aurora.store.data.event.UpdateEventListener;
import com.aurora.store.data.handler.ProgressHandler;
import com.aurora.store.util.Common;
import com.aurora.store.view.epoxy.views.UpdateModeView;
import com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StartActivity extends AppCompatActivity implements UpdateEventListener {

    IDeviceOwnerService iDOS;
    DevicePolicyManager dPM;
    ProgressDialog pd;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        /* ネットワークチェック */
        if (!isNetWork()) {
            netWorkError();
            return;
        }
        updateCheck();
    }

    /* ネットワークの接続を確認 */
    private boolean isNetWork() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /* ネットワークエラー */
    private void netWorkError() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_cpad_title_start_error)
                .setMessage(R.string.dialog_cpad_error_wifi)
                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> {
                    if (dPM.isDeviceOwnerApp(getPackageName())) {
                        new AlertDialog.Builder(this)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_cpad_clear_device_owner)
                                .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog2, which2) -> {
                                    dPM.clearDeviceOwnerApp(getPackageName());
                                    finishAndRemoveTask();
                                })
                                .setNegativeButton(R.string.dialog_cpad_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                .show();
                    } else finishAndRemoveTask();
                })
                .show();
    }

    private void updateCheck() {
        showLoadingDialog();
        new Updater(this).updateCheck();
    }

    private void supportCheck() {
        new Checker(this, Constants.CPAD_SUPPORT_CHECK_URL).supportCheck();
    }

    public void onSupportAvailable() {
        cancelLoadingDialog();
        showSupportDialog();
    }

    public void onSupportUnavailable() {
        cancelLoadingDialog();
        if (checkModel()) {
            if (!dPM.isDeviceOwnerApp(getPackageName())) {
                if (bindDeviceOwnerService()) {
                    Runnable runnable = this::isDeviceOwner;
                    new Handler().postDelayed(runnable, 1000);
                } else {
                    new AlertDialog.Builder(this)
                            .setCancelable(false)
                            .setTitle(R.string.dialog_cpad_title_start_error)
                            .setMessage(R.string.dialog_cpad_error_failure_bind)
                            .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> finishAndRemoveTask())
                            .show();
                }
            } else {
                if (Common.GET_SETTINGS_FLAG(this) == Constants.CPAD_SETTINGS_NOT_COMPLETED) {
                    startCheck();
                } else {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }
            }
        } else {
            errorNotNEO();
        }
    }

    @Override
    public void onDownloadError() {
        cancelLoadingDialog();
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_cpad_title_update)
                .setMessage(R.string.dialog_cpad_error)
                .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog, which) -> finishAffinity())
                .show();
    }

    public void isDeviceOwner() {
        try {
            if (iDOS.isDeviceOwnerApp()) {
                if (Common.GET_SETTINGS_FLAG(this) == Constants.CPAD_SETTINGS_NOT_COMPLETED) {
                    startCheck();
                } else {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }
            } else {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(R.string.dialog_cpad_title_start_error)
                        .setMessage(R.string.dialog_cpad_error_bind_no_owner)
                        .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> finishAndRemoveTask())
                        .show();
            }
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_cpad_title_start_error)
                    .setMessage(getResources().getString(R.string.dialog_cpad_error) + "\n" + e.getMessage())
                    .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> finishAndRemoveTask())
                    .show();
        }
    }

    ServiceConnection sC = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            iDOS = IDeviceOwnerService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            iDOS = null;
        }
    };

    public boolean bindDeviceOwnerService() {
        try {
            return bindService(Common.BIND_CUSTOMIZE_TOOL, sC, Context.BIND_AUTO_CREATE);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public void onUpdateApkDownloadComplete() {
        new Handler().post(() -> new Updater(this).installApk());
    }

    @Override
    public void onUpdateAvailable(String string) {
        cancelLoadingDialog();
        showUpdateDialog(string);
    }

    @Override
    public void onUpdateUnavailable() {
        supportCheck();
    }

    @Override
    public void onConnectionError() {
        cancelLoadingDialog();
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_cpad_title_start_error)
                .setMessage(R.string.dialog_cpad_error_connection)
                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> {
                    if (dPM.isDeviceOwnerApp(getPackageName())) {
                        new AlertDialog.Builder(this)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_cpad_clear_device_owner)
                                .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog2, which2) -> {
                                    dPM.clearDeviceOwnerApp(getPackageName());
                                    finishAndRemoveTask();
                                })
                                .setNegativeButton(R.string.dialog_cpad_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                .show();
                    } else finishAndRemoveTask();
                })
                .show();
    }

    private void showUpdateDialog(String str) {
        View view = getLayoutInflater().inflate(R.layout.sheet_cpad_update, null);
        TextView tv = view.findViewById(R.id.cpad_update_info);
        tv.setText(str);
        view.findViewById(R.id.button_cpad_update_info).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.CPAD_UPDATE_INFO_URL)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
                ToastKt.toast(this, R.string.toast_cpad_unknown_activity);
            }
        });

        new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .setTitle(R.string.dialog_cpad_title_update)
                .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog, which) -> {
                    AsyncFileDownload asyncFileDownload = initFileLoader();
                    ProgressDialog progressDialog = new ProgressDialog(this);
                    progressDialog.setTitle(R.string.dialog_cpad_title_update);
                    progressDialog.setMessage(getString(R.string.progress_cpad_state_downloading_update_file));
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setProgress(0);
                    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_cpad_common_cancel), (dialog2, which2) -> {
                        asyncFileDownload.cancel(true);
                        finishAffinity();
                    });
                    if (!progressDialog.isShowing()) progressDialog.show();
                    ProgressHandler progressHandler = new ProgressHandler();
                    progressHandler.progressDialog = progressDialog;
                    progressHandler.asyncfiledownload = asyncFileDownload;
                    progressHandler.sendEmptyMessage(0);
                })
                .setNegativeButton(R.string.dialog_cpad_common_no, (dialog, which) -> finishAffinity())
                .setNeutralButton(R.string.dialog_cpad_title_settings, (dialog, which) -> {
                    dialog.dismiss();
                    setUpdateMode(str);
                })
                .show();
    }

    private AsyncFileDownload initFileLoader() {
        AsyncFileDownload asyncfiledownload = new AsyncFileDownload(this, Common.DOWNLOAD_FILE_URL, new File(getExternalCacheDir(), "update.apk"));
        asyncfiledownload.execute();
        return asyncfiledownload;
    }

    private void setUpdateMode(String s) {
        View v = getLayoutInflater().inflate(R.layout.layout_cpad_update_list, null);
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
        listView.setAdapter(new UpdateModeView.AppListAdapter(this, dataList));
        listView.setOnItemClickListener((parent, mView, position, id) -> {
            switch (position) {
                case 0 -> {
                    Common.SET_UPDATE_MODE(this, (int) id);
                    listView.invalidateViews();
                }
                case 1 -> {
                    if (((DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(getPackageName())) {
                        Common.SET_UPDATE_MODE(this, (int) id);
                        listView.invalidateViews();
                    } else {
                        new AlertDialog.Builder(this)
                                .setMessage(getString(R.string.dialog_cpad_error_not_work_mode))
                                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                }
                case 2 -> {
                    if (bindDeviceOwnerService()) {
                        Common.SET_UPDATE_MODE(this, 2);
                        listView.invalidateViews();
                    } else {
                        new AlertDialog.Builder(this)
                                .setMessage(getString(R.string.dialog_cpad_error_not_work_mode))
                                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                }
            }
        });
        new AlertDialog.Builder(this)
                .setView(v)
                .setCancelable(false)
                .setTitle(getString(R.string.dialog_cpad_title_select_mode))
                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> {
                    dialog.dismiss();
                    showUpdateDialog(s);
                })
                .show();
    }

    private void showSupportDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_cpad_title_start_error)
                .setMessage(R.string.dialog_cpad_error_not_use)
                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> {
                    if (dPM.isDeviceOwnerApp(getPackageName())) {
                        new AlertDialog.Builder(this)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_cpad_clear_device_owner)
                                .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog2, which2) -> {
                                    dPM.clearDeviceOwnerApp(getPackageName());
                                    finishAndRemoveTask();
                                })
                                .setNegativeButton(R.string.dialog_cpad_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                .show();
                    } else finishAndRemoveTask();
                })
                .show();
    }

    private void showLoadingDialog() {
        pd = ProgressDialog.show(this, "", getString(R.string.progress_cpad_state_connection), true);
        pd.show();
    }

    private void cancelLoadingDialog() {
        try {
            if (pd != null) pd.dismiss();
        } catch (Exception ignored) {
        }
    }

    /* 端末チェック */
    public boolean checkModel() {
        String[] modelName = {"TAB-A05-BD", "TAB-A05-BA1"};
        for (String string : modelName) {
            if (Objects.equals(string, Build.MODEL)) {
                return true;
            }
        }
        return false;
    }

    /* 端末チェックエラー */
    private void errorNotNEO() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_cpad_title_start_error)
                .setMessage(R.string.dialog_cpad_error_not_neo)
                .setPositiveButton(R.string.dialog_cpad_common_ok, (dialog, which) -> {
                    if (dPM.isDeviceOwnerApp(getPackageName())) {
                        new AlertDialog.Builder(this)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_cpad_clear_device_owner)
                                .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog2, which2) -> {
                                    dPM.clearDeviceOwnerApp(getPackageName());
                                    finishAndRemoveTask();
                                })
                                .setNegativeButton(R.string.dialog_cpad_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                .show();
                    } else finishAndRemoveTask();
                })
                .show();
    }

    /* 初回起動お知らせ */
    public void startCheck() {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_cpad_title_notice_start)
                .setMessage(R.string.dialog_cpad_notice_start)
                .setPositiveButton(R.string.dialog_cpad_agree, (dialog, which) -> {

                    Common.SET_SETTINGS_FLAG(Constants.CPAD_SETTINGS_COMPLETED, this);
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .setNegativeButton(R.string.dialog_cpad_disagree, (dialog, which) -> {
                    if (dPM.isDeviceOwnerApp(getPackageName())) {
                        new AlertDialog.Builder(this)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_cpad_clear_device_owner)
                                .setPositiveButton(R.string.dialog_cpad_common_yes, (dialog2, which2) -> {
                                    dPM.clearDeviceOwnerApp(getPackageName());
                                    finishAndRemoveTask();
                                })
                                .setNegativeButton(R.string.dialog_cpad_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                .show();
                    } else finishAndRemoveTask();
                })
                .create();
        alertDialog.show();
        ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.CPAD_REQUEST_UPDATE) {
            finishAndRemoveTask();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (iDOS != null) unbindService(sC);
    }
}