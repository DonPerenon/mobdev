package io.github.mobdev.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mobdev.Contact
import io.github.mobdev.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(contact: Contact, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contact.name ?: stringResource(R.string.no_name)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { pvs ->
        Box(Modifier.padding(pvs)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val phone = contact.phoneNumber ?: stringResource(R.string.no_phone)
                val email = contact.email ?: stringResource(R.string.no_email)
                Text(
                    text = "${stringResource(R.string.phone_label)}: $phone",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${stringResource(R.string.email_label)}: $email",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
