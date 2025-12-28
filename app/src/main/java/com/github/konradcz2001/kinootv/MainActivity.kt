package com.github.konradcz2001.kinootv

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.github.konradcz2001.kinootv.utils.AppConstants
import com.github.konradcz2001.kinootv.utils.VirtualCursor

/**
 * Main entry point for the application.
 * Handles background Firebase authentication and performs a hidden WebView-based login
 * to the target website to retrieve session cookies.
 */
class MainActivity : FragmentActivity() {

    // Credentials from BuildConfig
    private val MY_LOGIN = BuildConfig.APP_LOGIN
    private val MY_PASSWORD = BuildConfig.APP_PASSWORD
    private val FIREBASE_EMAIL = BuildConfig.FIREBASE_LOGIN
    private val FIREBASE_PASSWORD = BuildConfig.FIREBASE_PASSWORD

    private var userAgentString: String = ""

    // Virtual cursor for navigating WebView with a D-pad remote
    private lateinit var virtualCursor: VirtualCursor

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Authentication
        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            auth.signInWithEmailAndPassword(FIREBASE_EMAIL, FIREBASE_PASSWORD)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("Auth", "SUCCESS: Connected to database as: ${task.result.user?.uid}")
                    } else {
                        Log.e("Auth", "ERROR logging into database: ${task.exception?.message}")
                    }
                }
        }

        // Initialize WebView for headless/hidden login
        val webView = WebView(this)
        setContentView(webView)

        // Initialize Virtual Cursor (must be done after setContentView)
        virtualCursor = VirtualCursor(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        // Override default User-Agent with a modern desktop signature from AppConstants
        webView.settings.userAgentString = AppConstants.DEFAULT_USER_AGENT
        userAgentString = AppConstants.DEFAULT_USER_AGENT

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                if (url != null) {
                    // Inject credentials if on the login page
                    if (url.contains("logowanie")) {
                        val js = "document.getElementById('input-login').value = '$MY_LOGIN'; " +
                                "document.getElementById('input-password').value = '$MY_PASSWORD';"
                        webView.evaluateJavascript(js, null)

                        // Auto-scroll to ensure elements are loaded/visible
                        webView.evaluateJavascript("window.scrollTo(0, document.body.scrollHeight);", null)
                    }

                    // Check login status by inspecting DOM content for specific keywords
                    val checkLoginJs = "document.body.innerText.includes('Wyloguj') || document.body.innerText.includes('Zalogowany jako')"

                    view?.evaluateJavascript(checkLoginJs) { result ->
                        Log.d("MainActivity", "Is Logged In (JS check): $result")

                        // "true" string indicates successful login
                        if (result == "true") {
                            Log.d("MainActivity", "Logged in! Retrieving cookies...")
                            val cookies = CookieManager.getInstance().getCookie(url)

                            if (cookies != null) {
                                val prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
                                prefs.edit {
                                    putString(AppConstants.COOKIE_KEY, cookies)
                                    putString(AppConstants.USER_AGENT_KEY, userAgentString)
                                }

                                // Navigate to the main browsing screen
                                val intent = Intent(this@MainActivity, BrowseActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                }
            }
        }

        // Check if session cookie already exists to skip login flow
        val prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
        if (prefs.getString(AppConstants.COOKIE_KEY, null) != null) {
            val intent = Intent(this, BrowseActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            webView.loadUrl("https://filman.cc/logowanie")
        }
    }

    /**
     * Intercepts key events to control the virtual cursor.
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (virtualCursor.handleKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}