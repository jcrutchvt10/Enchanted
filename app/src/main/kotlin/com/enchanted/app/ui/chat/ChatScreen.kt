package com.enchanted.app.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.enchanted.app.domain.model.ConversationState
import com.enchanted.app.ui.chat.components.ChatInput
import com.enchanted.app.ui.chat.components.ConversationStatusView
import com.enchanted.app.ui.chat.components.EmptyConversationView
import com.enchanted.app.ui.chat.components.Header
import com.enchanted.app.ui.chat.components.MessageList
import com.enchanted.app.ui.chat.components.UnreachableBanner
import com.enchanted.app.ui.sidebar.SidebarContent
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCompletions: () -> Unit = {},
    onNavigateToVoice: () -> Unit = {},
    onNavigateToStudio: () -> Unit = {}
) {
    val conversations by viewModel.conversations.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val models by viewModel.models.collectAsState()
    val selectedConversation by viewModel.selectedConversation.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val conversationState by viewModel.conversationState.collectAsState()
    val isReachable by viewModel.isReachable.collectAsState()
    val userInitials by viewModel.userInitials.collectAsState()
    val showMenu by viewModel.showMenu.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(showMenu) {
        if (showMenu) drawerState.open() else drawerState.close()
    }

    LaunchedEffect(drawerState.isClosed) {
        if (!drawerState.isClosed) viewModel.dismissMenu()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.85f)) {
                SidebarContent(
                    conversations = conversations,
                    selectedConversation = selectedConversation,
                    onConversationTap = { viewModel.selectConversation(it) },
                    onConversationDelete = { viewModel.deleteConversation(it) },
                    onDeleteAll = { viewModel.deleteAllConversations() },
                    onSettingsTap = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    onCompletionsTap = {
                        scope.launch { drawerState.close() }
                        onNavigateToCompletions()
                    },
                    onVoiceTap = {
                        scope.launch { drawerState.close() }
                        onNavigateToVoice()
                    },
                    onStudioTap = {
                        scope.launch { drawerState.close() }
                        onNavigateToStudio()
                    }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ChatContent(
                messages = messages,
                models = models,
                selectedModel = selectedModel,
                conversationState = conversationState,
                isReachable = isReachable,
                userInitials = userInitials,
                onMenuTap = { viewModel.toggleMenu() },
                onNewConversationTap = { viewModel.newConversation() },
                onSendMessageTap = { prompt, imageData, trimId ->
                    viewModel.sendMessage(prompt, imageData, trimId)
                },
                onStopGenerateTap = { viewModel.stopGenerating() },
                onSelectModel = { viewModel.selectModel(it) }
            )
        }
    }
}

@Composable
private fun ChatContent(
    messages: List<com.enchanted.app.domain.model.Message>,
    models: List<com.enchanted.app.domain.model.LanguageModel>,
    selectedModel: com.enchanted.app.domain.model.LanguageModel?,
    conversationState: ConversationState,
    isReachable: Boolean,
    userInitials: String,
    onMenuTap: () -> Unit,
    onNewConversationTap: () -> Unit,
    onSendMessageTap: (String, ByteArray?, String?) -> Unit,
    onStopGenerateTap: () -> Unit,
    onSelectModel: (com.enchanted.app.domain.model.LanguageModel?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Header(
            onMenuTap = onMenuTap,
            onNewConversationTap = onNewConversationTap,
            models = models,
            selectedModel = selectedModel,
            onSelectModel = onSelectModel
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                EmptyConversationView(
                    onSendPrompt = { prompt ->
                        selectedModel?.let { onSendMessageTap(prompt, null, null) }
                    }
                )
            } else {
                MessageList(
                    messages = messages,
                    conversationState = conversationState,
                    userInitials = userInitials
                )
            }
        }

        // Removed ConversationStatusView from here as it's now inside MessageList

        if (!isReachable) {
            UnreachableBanner()
        }

        ChatInput(
            conversationState = conversationState,
            modelSupportsImages = selectedModel?.imageSupport ?: false,
            onSendMessage = { prompt, imageData ->
                onSendMessageTap(prompt, imageData, null)
            },
            onStopGenerate = onStopGenerateTap
        )
    }
}
