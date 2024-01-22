package com.aurora.store.data.installer

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.aurora.store.R
import com.aurora.store.data.service.DhizukuService
import com.aurora.store.data.service.IDhizukuService
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import com.rosan.dhizuku.api.DhizukuUserServiceArgs
import java.io.File

class DhizukuInstaller(context: Context) : SessionInstallerBase(context) {

    private var service: IDhizukuService? = null

    override fun install(packageName: String, files: List<Any>) {
        if (!Dhizuku.init(context)) {
            return
        }
        if (!Dhizuku.isPermissionGranted()) Dhizuku.requestPermission(object :
            DhizukuRequestPermissionListener() {
            override fun onRequestPermission(grantResult: Int) {
                bindUserService()
            }
        }) else bindUserService()

        val uriList = files.map {
            when (it) {
                is File -> getUri(it)
                is String -> getUri(File(it))
                else -> {
                    throw Exception("Invalid data, expecting listOf() File or String")
                }
            }
        }

        try {
            val runnable = Runnable {
                if (!service?.tryInstallPackages(packageName, uriList)!!) {
                    removeFromInstallQueue(packageName)
                    postError(
                        packageName,
                        context.getString(R.string.dialog_cpad_error),
                        null
                    )
                }
            }
            Handler(Looper.getMainLooper()).postDelayed(runnable, 5000)
        } catch (e: Exception) {
            removeFromInstallQueue(packageName)
            postError(
                packageName,
                e.localizedMessage,
                e.stackTraceToString()
            )
        }
    }

    fun bindUserService() {
        val args = DhizukuUserServiceArgs(ComponentName(context, DhizukuService::class.java))
        val bind = Dhizuku.bindUserService(args, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, iBinder: IBinder) {
                service = IDhizukuService.Stub.asInterface(iBinder)
            }

            override fun onServiceDisconnected(name: ComponentName) {
            }
        })
        if (bind) return
    }
}