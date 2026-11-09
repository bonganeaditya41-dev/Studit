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
import com.studyit.ui.components.BackButton
import com.studyit.ui.components.GlassCard
import com.studyit.ui.components.LoadingBox
import com.studyit.ui.components.PostCard
import com.studyit.data.model.VoteType

@Composable
fun CommunityDetailScreen(
    communityId: String,
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
    viewModel: CommunityDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(communityId) { viewModel.bind(communityId) }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val community = state.community
        if (community == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                BackButton(onBack = onBack)
                if (state.loading) LoadingBox() else
                    Text("Community not found", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onBack = onBack)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = viewModel::toggleMembership) {
                        Text(if (state.joined) "Leave" else "Join")
                    }
                }
            }
            item {
                GlassCard {
                    Text(
                        community.slug,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        community.memberCount.toString() + " members",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(community.description, style = MaterialTheme.typography.bodyMedium)
                    if (community.rules.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("Rules", style = MaterialTheme.typography.titleMedium)
                        community.rules.forEachIndexed { index, rule ->
                            Text(
                                (index + 1).toString() + ". " + rule,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (state.posts.isEmpty()) {
                item {
                    Text(
                        "No questions in this community yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            items(state.posts, key = { it.postId }) { post ->
                PostCard(
                    post = post,
                    myVote = state.votes[post.postId],
                    isBookmarked = false,
                    onClick = { onOpenPost(post.postId) },
                    onUpvote = { viewModel.vote(post, VoteType.UP) },
                    onDownvote = { viewModel.vote(post, VoteType.DOWN) },
                    onBookmark = { },
                    onShare = { },
                    onOpenAuthor = { }
                )
            }
        }
    }
}
