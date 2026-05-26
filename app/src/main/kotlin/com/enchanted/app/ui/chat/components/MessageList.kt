package com.enchanted.app.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enchanted.app.domain.model.ConversationState
import com.enchanted.app.domain.model.Message

@Composable
fun MessageList(
    messages: List<Message>,
    conversationState: ConversationState,
    userInitials: String,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll logic
    val lastMessageContentLength = remember(messages) { messages.lastOrNull()?.content?.length ?: 0 }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size) // scroll to last item (or status)
        }
    }

    // Immediate scroll when content grows (during stream)
    // Only scroll if the user is already at the bottom or very close to it
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                true
            } else {
                val lastVisibleItem = visibleItemsInfo.lastOrNull()
                lastVisibleItem?.index != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 2
            }
        }
    }

    LaunchedEffect(lastMessageContentLength) {
        if (messages.isNotEmpty() && isAtBottom && !messages.last().done) {
            listState.scrollToItem(messages.size) // Stay at the bottom/status view
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(
            items = messages,
            key = { it.id.toString() }
        ) { message ->
            MessageBubble(
                message = message,
                userInitials = userInitials
            )
        }

        item(key = "status") {
            Column {
                ConversationStatusView(state = conversationState)
                Spacer(modifier = Modifier.height(16.dp)) // Padding at bottom of list
            }
        }
    }
}
