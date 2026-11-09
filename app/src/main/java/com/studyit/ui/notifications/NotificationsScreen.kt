package com.studyit.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyit.core.timeAgo
import com.studyit.ui.components.EmptyState
import com.studyit.ui.components.GlassCard
import com.studyit.ui.components.LoadingBox

@Composable
fun NotificationsScreen(
    onOpenPost: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 8.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Notifications", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (state.unread == 0) "You are all caught up"
                    else state.unread.toString() + " unread",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.unread > 0) {
                TextButton(onClick = viewModel::markAllRead) { Text("Mark all read") }
            }
        }

        if (state.loading) {
            LoadingBox()
        } else if (state.notifications.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Notifications,
                title = "Nothing here yet",
                message = "Answers, replies and upvotes on your contributions will appear here."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.notifications, key = { it.notificationId }) { notification ->
                    GlassCard(onClick = {
                        viewModel.markRead(notification)
                        if (notification.postId.isNotBlank()) onOpenPost(notification.postId)
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    notification.senderName.ifBlank { "Studit" },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (notification.isRead) FontWeight.Normal
                                    else FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    notification.message,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    notification.createdAt.timeAgo(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!notification.isRead) {
                                Text(
                                    "New",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
