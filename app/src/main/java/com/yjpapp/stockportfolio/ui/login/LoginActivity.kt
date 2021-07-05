package com.yjpapp.stockportfolio.ui.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.yjpapp.stockportfolio.R
import com.yjpapp.stockportfolio.base.BaseMVVMActivity
import com.yjpapp.stockportfolio.constance.AppConfig
import com.yjpapp.stockportfolio.databinding.ActivityLoginBinding

/**
 * @author 윤재박
 * @since 2021.07
 */
class LoginActivity : BaseMVVMActivity() {
    private val TAG = LoginActivity::class.simpleName
    private val binding = binding<ActivityLoginBinding>(R.layout.activity_login)
    private val gso by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(AppConfig.GOOGLE_SIGN_CLIENT_ID)
            .requestEmail() // email addresses도 요청함
            .build()
    }
    private val mGoogleSignInClient by lazy { GoogleSignIn.getClient(applicationContext, gso) }

    interface LoginCallBack {
        fun onClick(view: View)
    }

    private val loginCallBack = object : LoginCallBack {
        override fun onClick(view: View) {
            when (view.id) {
                binding.value.btnGoogleLogin.id -> {
                    googleSignIn()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initData()
    }

    private fun initView() {

    }

    private fun initData() {
        binding.value.apply {
            lifecycleOwner = this@LoginActivity
            callback = loginCallBack
        }
    }


    private fun googleSignIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        googleLoginResult.launch(signInIntent)
    }

    private val googleLoginResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_CANCELED) {
            val task: Task<GoogleSignInAccount> =
                GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {
                val acct: GoogleSignInAccount? = task.getResult(ApiException::class.java)
                acct?.let {
                    val personName = it.displayName
                    val personGivenName = it.givenName
                    val personFamilyName = it.familyName
                    val personEmail = it.email
                    val personId = it.id
                    val personPhoto: Uri? = it.photoUrl
                    Log.d(TAG, "handleSignInResult:personName $personName")
                    Log.d(TAG, "handleSignInResult:personGivenName $personGivenName")
                    Log.d(TAG, "handleSignInResult:personEmail $personEmail")
                    Log.d(TAG, "handleSignInResult:personId $personId")
                    Log.d(TAG, "handleSignInResult:personFamilyName $personFamilyName")
                    Log.d(TAG, "handleSignInResult:personPhoto $personPhoto")
                }
            } catch (e: ApiException) {
                // The ApiException status code indicates the detailed failure reason.
                // Please refer to the GoogleSignInStatusCodes class reference for more information.
                Log.e(TAG, "signInResult:failed code=" + e.statusCode)
            }
        }
    }
}