package io.github.x80070006.holowave

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import io.github.x80070006.holowave.ui.HolowaveRoot
import io.github.x80070006.holowave.ui.MainViewModel
import io.github.x80070006.holowave.ui.screens.WelcomeScreen
import io.github.x80070006.holowave.ui.theme.HolowaveTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private var granted by mutableStateOf(false)
    private var permanentlyDenied by mutableStateOf(false)

    private val audioPermission =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        refreshPermission()
        if (result[audioPermission] == false) {
            permanentlyDenied = !shouldShowRequestPermissionRationale(audioPermission)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        refreshPermission()
        setContent {
            HolowaveTheme {
                if (granted) HolowaveRoot(vm)
                else WelcomeScreen(
                    permanentlyDenied = permanentlyDenied,
                    onRequest = ::askPermissions,
                    onOpenSettings = ::openAppSettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermission()
    }

    private fun refreshPermission() {
        val now = ContextCompat.checkSelfPermission(this, audioPermission) == PackageManager.PERMISSION_GRANTED
        if (now && !granted) vm.onPermissionGranted()
        granted = now
        if (now) permanentlyDenied = false
    }

    private fun askPermissions() {
        val wanted = buildList {
            add(audioPermission)
            // Needed for the playback notification on Android 13+; refusing it does not block playback.
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissions.launch(wanted.toTypedArray())
    }

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
