package io.github.mobdev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mobdev.chat.data.repository.ChatRepository
import io.github.mobdev.chat.data.session.CredentialsStore
import io.github.mobdev.chat.data.session.SessionManager
import io.github.mobdev.chat.ui.ChatApp
import io.github.mobdev.chat.ui.ChatViewModel
import io.github.mobdev.ui.theme.MobdevTheme

class MainActivity : ComponentActivity() {

    private val sessionManager by lazy { SessionManager() }
    private val chatRepository by lazy { ChatRepository.create(applicationContext, sessionManager) }
    private val credentialsStore by lazy { CredentialsStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobdevTheme {
                val viewModel: ChatViewModel = viewModel(
                    factory = ChatViewModel.Factory(chatRepository, credentialsStore),
                )
                ChatApp(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        chatRepository.networkMonitor.stop()
        super.onDestroy()
    }
}
