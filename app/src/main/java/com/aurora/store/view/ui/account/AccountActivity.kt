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

package com.aurora.store.view.ui.account

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.aurora.extensions.browse
import com.aurora.extensions.close
import com.aurora.extensions.load
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.store.R
import com.aurora.store.data.AuthState
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.ActivityAccountBinding
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.viewmodel.auth.AuthViewModel
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class AccountActivity : BaseActivity() {

    private lateinit var VM: AuthViewModel
    private lateinit var B: ActivityAccountBinding

    private lateinit var authData: AuthData
    private lateinit var accountProvider: AccountProvider

    private val URL_TOS = "https://www.google.com/mobile/android/market-tos.html"
    private val URL_LICENSE = "https://gitlab.com/AuroraOSS/AuroraStore/raw/master/LICENSE"
    private val URL_DISCLAIMER = "https://gitlab.com/AuroraOSS/AuroraStore/raw/master/DISCLAIMER.md"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EventBus.getDefault().register(this);

        B = ActivityAccountBinding.inflate(layoutInflater)
        VM = ViewModelProvider(this).get(AuthViewModel::class.java)

        setContentView(B.root)

        authData = AuthProvider.with(this).getAuthData()
        accountProvider = AccountProvider.with(this)

        attachToolbar()
        attachChips()
        attachActions()

        updateContents()

        VM.liveData.observe(this, {
            when (it) {
                AuthState.Valid -> {

                }

                AuthState.Available -> {
                    updateStatus("セッションの確認中...")
                    updateActionLayout(false)
                }

                AuthState.Unavailable -> {
                    updateStatus("最初にログインする必要があります")
                    updateActionLayout(true)
                }

                AuthState.SignedIn -> {
                    updateContents()
                }

                AuthState.SignedOut -> {
                    updateStatus("最後のセッションは破棄されました")
                    updateActionLayout(true)
                }

                is AuthState.Status -> {
                    updateStatus(it.status)
                }
            }
        })
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe()
    fun onEventReceived(event: BusEvent) {
        when (event) {
            is BusEvent.GoogleAAS -> {
                if (event.success) {
                    updateStatus("Googleセッションの確認中...")
                    VM.buildGoogleAuthData(event.email, event.aasToken)
                } else {
                    updateStatus("Google経由でのログインに失敗しました")
                }
            }
            else -> {

            }
        }
    }

    private fun updateContents() {
        if (accountProvider.isSignedIn()) {
            B.viewFlipper.displayedChild = 1
            updateStatus("すべて良いです")
        } else {
            B.viewFlipper.displayedChild = 0
            updateStatus("ログインしてお楽しみください")
        }

        updateUserProfile()
    }

    private fun updateStatus(string: String?) {
        runOnUiThread {
            B.txtStatus.apply {
                text = string
            }
        }
    }

    private fun updateActionLayout(isVisible: Boolean) {
        if (isVisible) {
            B.layoutAction.visibility = View.VISIBLE
        } else {
            B.layoutAction.visibility = View.INVISIBLE
        }
    }

    private fun attachToolbar() {
        B.layoutToolbarAction.txtTitle.text = getString(R.string.title_account_manager)
        B.layoutToolbarAction.imgActionPrimary.setOnClickListener {
            close()
        }
    }

    private fun attachChips() {
        B.chipDisclaimer.setOnClickListener { browse(URL_DISCLAIMER) }
        B.chipLicense.setOnClickListener { browse(URL_LICENSE) }
        B.chipTos.setOnClickListener { browse(URL_TOS) }
    }


    private fun attachActions() {
        B.btnAnonymous.updateProgress(false)
        B.btnGoogle.updateProgress(false)

        B.btnAnonymous.addOnClickListener {
            B.btnAnonymous.updateProgress(true)
            VM.buildAnonymousAuthData()
        }

        B.btnGoogle.addOnClickListener {
            B.btnGoogle.updateProgress(true)
            openGoogleActivity()
        }

        B.btnLogout.addOnClickListener {
            task {
                AccountProvider.with(this).logout()
            } successUi {
                B.btnAnonymous.updateProgress(false)
                B.btnGoogle.updateProgress(false)
                updateContents()
            } failUi {

            }
        }
    }

    private fun updateUserProfile() {
        authData = AuthProvider.with(this).getAuthData()

        if (accountProvider.isSignedIn()) {
            authData.userProfile?.let {
                B.imgAvatar.load(it.artwork.url) {
                    placeholder(R.drawable.bg_placeholder)
                    transform(RoundedCorners(32))
                }

                B.txtName.text = if (authData.isAnonymous)
                    "匿名"
                else
                    it.name

                B.txtEmail.text = it.email
            }
        } else {
            B.imgAvatar.load(R.mipmap.ic_launcher) {
                transform(RoundedCorners(32))
            }
            B.txtName.text = getString(R.string.app_name)
            B.txtEmail.text = getString(R.string.account_logged_out)
        }
    }

    override fun onConnected() {
        hideNetworkConnectivitySheet()
    }

    override fun onDisconnected() {
        showNetworkConnectivitySheet()
    }

    override fun onReconnected() {

    }
}