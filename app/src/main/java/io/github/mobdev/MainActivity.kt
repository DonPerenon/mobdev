package io.github.mobdev

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.github.mobdev.ui.ContactDetailScreen
import io.github.mobdev.ui.ContactListScreen
import io.github.mobdev.ui.theme.MobdevTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobdevTheme {
                MainContent()
            }
        }
    }
}

@Composable
private fun MainContent() {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) permissionDenied = true
    }

    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            contacts = withContext(Dispatchers.IO) { context.fetchAllContacts() }
        }
    }

    if (selectedContact != null) {
        ContactDetailScreen(
            contact = selectedContact!!,
            onBack = { selectedContact = null }
        )
    } else {
        ContactListScreen(
            hasPermission = hasPermission,
            contacts = contacts,
            onRequestPermission = {
                val activity = context as Activity
                val canAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.READ_CONTACTS
                )
                if (permissionDenied && !canAskAgain) {
                    // Диалог навсегда заблокирован — открываем настройки приложения
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            },
            onContactClick = { selectedContact = it }
        )
    }
}
