package com.studyit.ui.communities

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyit.ui.components.EmptyState
import com.studyit.ui.components.GlassCard
import com.studyit.ui.components.LoadingBox
import com.studyit.ui.components.StuditTextField

@Composable
fun CommunitiesScreen(
    onOpenCommunity: (String) -> Unit,
    onCreateCommunity: () -> Unit,
    viewModel: CommunitiesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateCommunity,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New space") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Communities", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                StuditTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    label = "Search communities",
                    placeholder = "Physics, NEET, Class 12…"
                )
            }

            if (state.loading) {
                LoadingBox()
            } else if (state.visible.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.AccountCircle,
                    title = "No communities found",
                    message = "Create a space for your subject or exam prep group.",
                    actionLabel = "Create community",
                    onAction = onCreateCommunity
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.visible, key = { it.communityId }) { community ->
                        val joined = state.myCommunityIds.contains(community.communityId)
                        GlassCard(onClick = { onOpenCommunity(community.communityId) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        community.slug,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        community.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        community.memberCount.toString() + " members",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { viewModel.toggleMembership(community) }) {
                                    Text(if (joined) "Leave" else "Join")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
