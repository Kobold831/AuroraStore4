package com.aurora.store.data.installer

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.aurora.store.R
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.service.DhizukuService
import com.aurora.store.data.service.IDhizukuService
import com.aurora.store.util.Common
import com.aurora.store.util.Log
import com.aurora.store.util.PackageUtil
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuUserServiceArgs

class DhizukuInstaller(context: Context) : InstallerBase(context) {

    private var mDhizukuService: IDhizukuService? = null

    override fun install(download: Download) {
        if (isAlreadyQueued(download.packageName)) {
            Log.i("${download.packageName} already queued")
        } else {
            if (!Dhizuku.init(context)) {
                return
            }

            if (Common.isDhizukuActive(context)) {
                tryBindDhizukuService()
            } else {
                return
            }

            download.sharedLibs.forEach {
                if (!PackageUtil.isSharedLibraryInstalled(
                        context,
                        it.packageName,
                        it.versionCode
                    )
                ) {
                    TODO("AIDL修正予定")
                }
            }

            try {
                val runnable = Runnable {
                    if (!mDhizukuService?.tryInstallPackages(
                            Common.convertToFilePath(
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
                Handler(Looper.getMainLooper()).postDelayed(runnable, 5000)
            } catch (e: Exception) {
                removeFromInstallQueue(download.packageName)
                postError(
                    download.packageName,
                    e.localizedMessage,
                    e.stackTraceToString()
                )
            }

        }
    }

    private fun tryBindDhizukuService() {
        val args = DhizukuUserServiceArgs(ComponentName(context, DhizukuService::class.java))
        val bind = Dhizuku.bindUserService(args, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, iBinder: IBinder) {
                mDhizukuService = IDhizukuService.Stub.asInterface(iBinder)
            }

            override fun onServiceDisconnected(name: ComponentName) {
            }
        })
        if (bind) return
    }
}