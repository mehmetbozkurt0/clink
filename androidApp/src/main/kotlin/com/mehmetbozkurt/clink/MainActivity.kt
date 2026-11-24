package com.mehmetbozkurt.clink

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    private val incomingLinks = IncomingLinks()

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is dark only, so system bar icons are always light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // An invite link that started the app; a recreated activity has already handled it.
        if (savedInstanceState == null) incomingLinks.receive(intent?.dataString)
        val platform = Platform(
            contentCache = FileContentCache(applicationContext),
            localStore = FileLocalStore(applicationContext),
            googleIdTokens = CredentialManagerGoogleIdTokenProvider(this),
            share = { text -> shareText(this, text) },
            incomingLinks = incomingLinks,
            language = AndroidLanguageSetting(this),
            versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty(),
            // "Remove animations" in the phone's accessibility settings sets this scale to 0.
            reduceMotion = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f,
        )
        setContent { App(platform) }
    }

    // Below Android 13 the app's own language is applied here (Android 13+ does it itself).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AndroidLanguageSetting.wrap(newBase))
    }

    // An invite link opened while the app is already running (launchMode singleTop).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incomingLinks.receive(intent.dataString)
    }
}
