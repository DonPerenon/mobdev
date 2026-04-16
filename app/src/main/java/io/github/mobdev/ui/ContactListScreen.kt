package io.github.mobdev.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mobdev.Contact
import io.github.mobdev.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    hasPermission: Boolean,
    contacts: List<Contact>,
    onRequestPermission: () -> Unit,
    onContactClick: (Contact) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.contacts_title)) })
        }
    ) { pvs ->
        Box(Modifier.padding(pvs)) {
            if (hasPermission) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(contacts) { contact ->
                        ListItem(
                            headlineContent = {
                                Text(contact.name ?: stringResource(R.string.no_name))
                            },
                            modifier = Modifier.clickable { onContactClick(contact) }
                        )
                        HorizontalDivider()
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(stringResource(R.string.permission_not_granted))
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRequestPermission) {
                        Text(stringResource(R.string.grant_permission))
                    }
                }
            }
        }
    }
}
