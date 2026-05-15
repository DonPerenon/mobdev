package io.github.mobdev.chat.ui

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mobdev.R
import io.github.mobdev.chat.ui.chats.ChatListScreen
import io.github.mobdev.chat.ui.image.FullImageScreen
import io.github.mobdev.chat.ui.login.LoginScreen
import io.github.mobdev.chat.ui.messages.MessagesScreen

@Composable
fun ChatApp(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activity = LocalContext.current as Activity

    val errorText = state.errorDialogMessage?.let { key ->
        when (key) {
            ChatViewModel.ERROR_EMPTY_CREDENTIALS ->
                stringResource(R.string.error_empty_credentials)
            ChatViewModel.ERROR_INVALID_CREDENTIALS ->
                stringResource(R.string.error_invalid_credentials)
            ChatViewModel.ERROR_UNAUTHORIZED ->
                stringResource(R.string.error_unauthorized)
            else -> stringResource(R.string.error_network)
        }
    }

    if (errorText != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text(stringResource(R.string.error_dialog_title)) },
            text = { Text(errorText) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissError) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }

    BackHandler {
        when {
            state.fullImagePath != null -> viewModel.closeFullImage()
            isLandscape && state.selectedChannel != null -> viewModel.closeChannel()
            !isLandscape && state.portraitScreen == PortraitScreen.Messages ->
                viewModel.navigateToChatList()
            state.portraitScreen == PortraitScreen.Login -> activity.finish()
            state.portraitScreen == PortraitScreen.ChatList -> activity.finish()
            else -> activity.finish()
        }
    }

    if (state.fullImagePath != null) {
        FullImageScreen(
            imageUrl = viewModel.imageUrl(state.fullImagePath!!, fullResolution = true),
            onClose = viewModel::closeFullImage,
        )
        return
    }

    if (!state.skipLoginChecked) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            ChatListScreen(
                channels = state.channels,
                selectedChannel = state.selectedChannel,
                isLoading = state.isLoadingChannels,
                isRefreshingSession = state.isLoggingIn,
                onChannelClick = viewModel::openChannel,
                onLogoutClick = viewModel::logout,
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
            )
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
            ) {
                val channel = state.selectedChannel
                if (channel == null) {
                    Text(
                        text = stringResource(R.string.select_chat),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    MessagesScreen(
                        channelName = channel,
                        messages = state.messages,
                        messageInput = state.messageInput,
                        isLoading = state.isLoadingMessages,
                        isLoadingMore = state.isLoadingMore,
                        isSending = state.isSending,
                        showBackToChats = false,
                        imageUrl = viewModel::imageUrl,
                        onMessageInputChange = viewModel::onMessageInputChange,
                        onSendClick = viewModel::sendMessage,
                        onImageClick = viewModel::openFullImage,
                        onBackToChats = viewModel::closeChannel,
                        onLoadMore = viewModel::loadMoreMessages,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        return
    }

    when (state.portraitScreen) {
        PortraitScreen.Login -> {
            LoginScreen(
                username = state.username,
                password = state.password,
                isLoggingIn = state.isLoggingIn,
                onUsernameChange = viewModel::onUsernameChange,
                onPasswordChange = viewModel::onPasswordChange,
                onLoginClick = viewModel::login,
            )
        }
        PortraitScreen.ChatList -> {
            ChatListScreen(
                channels = state.channels,
                selectedChannel = state.selectedChannel,
                isLoading = state.isLoadingChannels,
                isRefreshingSession = state.isLoggingIn,
                onChannelClick = viewModel::openChannel,
                onLogoutClick = viewModel::logout,
            )
        }
        PortraitScreen.Messages -> {
            val channel = state.selectedChannel
            if (channel != null) {
                MessagesScreen(
                    channelName = channel,
                    messages = state.messages,
                    messageInput = state.messageInput,
                    isLoading = state.isLoadingMessages,
                    isLoadingMore = state.isLoadingMore,
                    isSending = state.isSending,
                    showBackToChats = true,
                    imageUrl = viewModel::imageUrl,
                    onMessageInputChange = viewModel::onMessageInputChange,
                    onSendClick = viewModel::sendMessage,
                    onImageClick = viewModel::openFullImage,
                    onBackToChats = viewModel::navigateToChatList,
                    onLoadMore = viewModel::loadMoreMessages,
                )
            }
        }
    }
}
