package com.dailypush.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private lateinit var state: AppState

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        state = AppState(applicationContext)
        state.refresh()

        Reminders.ensureChannel(this)
        Reminders.scheduleAll(this)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val actions = Actions(
            testNotification = {
                val ok = Reminders.showTest(this)
                if (!ok) {
                    Toast.makeText(
                        this,
                        "নোটিফিকেশনের পারমিশন বন্ধ আছে। অ্যাপ সেটিংস থেকে চালু করো।",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            openAppSettings = {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:$packageName"))
                )
            }
        )

        setContent {
            AppTheme {
                DailyPushApp(state, actions)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::state.isInitialized) state.refresh()
    }
}
