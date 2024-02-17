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

package com.aurora.store.data.installer

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.IBinder
import androidx.core.content.IntentCompat
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.NotificationUtil
import org.greenrobot.eventbus.EventBus

class InstallerService : Service() {

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -69)
        val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
        val extra = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

        if (CommonUtil.inForeground() && status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            promptUser(intent)
        } else {
            postStatus(status, packageName, extra)
            notifyUser(packageName!!, status)
        }

        stopSelf()
        return START_NOT_STICKY
    }

    private fun notifyUser(packageName: String, status: Int) {
        val notificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationUtil.getInstallerStatusNotification(
            this,
            App(packageName),
            AppInstaller.getErrorString(this, status)
        )
        notificationManager.notify(packageName.hashCode(), notification)
    }

    @SuppressLint("UnsafeIntentLaunch")
    private fun promptUser(intent: Intent) {
        IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)?.let {
            it.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            it.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            try {
                this.startActivity(it)
            } catch (_: Exception) {
            }
        }
    }

    private fun postStatus(status: Int, packageName: String?, extra: String?) {
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                EventBus.getDefault().post(
                    InstallerEvent.Success(
                        packageName,
                        this.getString(R.string.installer_status_success)
                    )
                )
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                EventBus.getDefault().post(
                    InstallerEvent.Cancelled(
                        packageName,
                        AppInstaller.getErrorString(this, status)
                    )
                )
            }

            else -> {
                EventBus.getDefault().post(
                    InstallerEvent.Failed(
                        packageName,
                        AppInstaller.getErrorString(this, status),
                        extra
                    )
                )
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}