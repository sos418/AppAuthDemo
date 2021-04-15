package com.example.appauth


import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_login.*
import net.openid.appauth.*
import net.openid.appauth.AuthState.AuthStateAction
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private val RC_AUTH = 100

    private var mAuthService: AuthorizationService? = null
    private var mStateManager: AuthStateManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mStateManager = AuthStateManager.getInstance(this)
        mAuthService = AuthorizationService(this)

        if (mStateManager?.current?.isAuthorized!!) {
            button_login.setText("Logout")
            mStateManager?.current?.performActionWithFreshTokens(
                mAuthService!!
            ) { accessToken, idToken, exception ->
                ProfileTask().execute(accessToken)
            }
        }else{
            button_login.setText("Login")
        }

        /*
        OIDC串接Fetnet登入頁面
         */
        button_login.setOnClickListener {
            if (mStateManager?.current?.isAuthorized!!) {
                val url = "https://login2-dev.fetnet.net/logout/logout"
                val builder = CustomTabsIntent.Builder()
                val customTabsIntent = builder.build()
                mStateManager!!.replace(AuthState())
                button_login.setText("Login")
                customTabsIntent.launchUrl(this, Uri.parse(url))

            } else {
                val serviceConfig = AuthorizationServiceConfiguration(
                    Uri.parse("https://login2-dev.fetnet.net/mga/sps/oauth/oauth20/authorize"), // authorization endpoint
                    Uri.parse("https://login2-dev.fetnet.net/mga/sps/oauth/oauth20/token") // token endpoint
                )

                //OIDC ClientID
                val clientId = "TryWTHawm7Bk95FEZmZ6"
//                val clientId = "zNcvR5BGtWibH7GzYvqp"
                val redirectUri = Uri.parse("com.sdp.appauth:/oauth2callback")
                val builder = AuthorizationRequest.Builder(
                    serviceConfig,
                    clientId,
                    ResponseTypeValues.CODE,
                    redirectUri
                )
                builder.setScopes("openid")

                val authRequest = builder.build()
                val authService = AuthorizationService(this)
                val authIntent = authService.getAuthorizationRequestIntent(authRequest)
                startActivityForResult(authIntent, RC_AUTH)
            }
        }

        /*
        Fido註冊測試頁面，使用chrome開啟瀏覽器
         */
        button_fido_register.setOnClickListener {
            val PACKAGE_NAME="com.android.chrome"
//            val PACKAGE_NAME="org.mozilla.firefox"

            val packageManager = packageManager

            val url = "https://login2-dev.fetnet.net/fetidManager/fido2Register"
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            val intent = customTabsIntent.intent
            intent.data = Uri.parse(url)

            val resolveInfoList = packageManager.queryIntentActivities(customTabsIntent.intent, PackageManager.MATCH_ALL)
            for(resolveInfo in resolveInfoList){
                val packageName = resolveInfo.activityInfo.packageName
                if(TextUtils.equals(packageName, PACKAGE_NAME)){
                    customTabsIntent.intent.setPackage(PACKAGE_NAME)
                }
            }
            customTabsIntent.launchUrl(this, customTabsIntent.intent.data!!)
        }

        /*
        Fido登入測試頁面(只有Usernameless流程)
         */
        button_fido_login.setOnClickListener {
            val url = "https://login2-dev.fetnet.net/fetidManager/fido2Login"
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(this, Uri.parse(url))
        }

        /*
        Fido登入測試頁面(Usernameless+Passwordless流程)
         */
        button_fido_login2.setOnClickListener {
            val url = "https://login2-dev.fetnet.net/fetidManager/fido2Login2"
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(this, Uri.parse(url))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_AUTH) {
            val resp = AuthorizationResponse.fromIntent(data!!)
            val ex = AuthorizationException.fromIntent(data)

            if (resp != null) {
                mAuthService = AuthorizationService(this)
                mStateManager?.updateAfterAuthorization(resp, ex)

                val clientAuth = ClientSecretBasic("2IdUHRFB8z2xRte2ZToV")
//                val clientAuth = ClientSecretBasic("cBgUoZBYkT1ljG1cmwgH")
                mAuthService?.performTokenRequest(
                    resp.createTokenExchangeRequest(), clientAuth
                ) { resp, ex ->
                    if (resp != null) {
                        mStateManager?.updateAfterTokenResponse(resp, ex)
                        button_login.setText("Logout")
                        Log.d("Authentication Response:", resp.jsonSerializeString())
                        Log.d("accessToken", resp.accessToken)
                        ProfileTask().execute(resp.accessToken)
                    } else {
                        Log.e("Auth fail :", ex.toString())
                    }
                }

                // authorization completed
            } else {
                Log.e("Auth fail:", ex.toString())
            }
        } else {
            // ...
        }
        if (mStateManager?.current?.isAuthorized!!) {
            button_login.text = "Logout"
            mStateManager?.current?.performActionWithFreshTokens(
                mAuthService!!
            ) { accessToken, idToken, exception ->
                ProfileTask().execute(accessToken)
            }

        }
    }

    inner class ProfileTask : AsyncTask<String?, Void, JSONObject>() {
        override fun doInBackground(vararg tokens: String?): JSONObject? {
            // 可以拿token呼叫API等...
            return null
        }
    }

}
