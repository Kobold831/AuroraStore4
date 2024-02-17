package com.aurora.store.data.installer

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.aurora.store.R
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.Common
import com.aurora.store.util.Log
import com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService

class DelegationInstaller(context: Context) : InstallerBase(context) {

    private var mDeviceOwnerService: IDeviceOwnerService? = null

    override fun install(download: Download) {
        if (isAlreadyQueued(download.packageName)) {
            Log.i("${download.packageName} already queued")
        } else {
            if (tryBindDeviceOwnerService()) {
                try {
                    val runnable = Runnable {
                        if (!mDeviceOwnerService?.tryInstallPackages(
                                download.packageName, Common.convertToUri(
                                    getFiles(
                                        download.packageName,
                                        download.versionCode
                                    )
                                )
                            )!!
                        ) {
                            removeFromInstallQueue(download.packageName)
                            postError(
                                download.packageName,
                                context.getString(R.string.dialog_cpad_error),
                                null
                            )
                        }
                    }
                    Handler(Looper.getMainLooper()).postDelayed(runnable, 1000)
                } catch (e: Exception) {
                    removeFromInstallQueue(download.packageName)
                    postError(
                        download.packageName,
                        e.localizedMessage,
                        e.stackTraceToString()
                    )
                }
            } else {
                removeFromInstallQueue(download.packageName)
                postError(
                    download.packageName,
                    context.getString(R.string.dialog_cpad_error_failure_connection),
                    null
                )
            }
        }
    }

    private fun tryBindDeviceOwnerService(): Boolean {
        return try {
            context.bindService(
                Common.CUSTOMIZE_TOOL_SERVICE,
                mDeviceOwnerServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        } catch (ignored: Exception) {
            false
        }
    }

    private var mDeviceOwnerServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            mDeviceOwnerService = IDeviceOwnerService.Stub.asInterface(iBinder)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
        }
    }
}