package io.github.mobdev.chat.ui.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mobdev.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    channels: List<String>,
    selectedChannel: String?,
    isLoading: Boolean,
    isRefreshingSession: Boolean = false,
    onChannelClick: (String) -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(title ?: stringResource(R.string.chats_title))
                },
                actions = {
                    TextButton(onClick = onLogoutClick) {
                        Text(stringResource(R.string.logout))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                isLoading || isRefreshingSession -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                channels.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.chats_empty),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(channels, key = { it }) { channel ->
                            val selected = channel == selectedChannel
                            Text(
                                text = channel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                    )
                                    .clickable { onChannelClick(channel) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
