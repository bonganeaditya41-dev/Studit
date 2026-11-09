package com.studyit.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyit.core.shareText
import com.studyit.data.model.FeedSort
import com.studyit.ui.components.EmptyState
import com.studyit.ui.components.ErrorState
import com.studyit.ui.components.LoadingBox
import com.studyit.ui.components.PostCard

@Composable
fun HomeScreen(
    onOpenPost: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Studit",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Your study feed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenLeaderboard) {
                    Icon(Icons.Default.Star, contentDescription = "Leaderboard")
                }
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh feed")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeedSort.entries.forEach { sort ->
                    FilterChip(
                        selected = state.sort == sort,
                        onClick = { viewModel.setSort(sort) },
                        label = { Text(sort.label) },
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            when {
                state.loading -> LoadingBox()
                state.error != null -> ErrorState(state.error.orEmpty()) { viewModel.refresh() }
                state.posts.isEmpty() -> EmptyState(
                    icon = Icons.Default.List,
                    title = "No discussions yet",
                    message = "Be the first to ask a question in your community.",
                    actionLabel = "Refresh",
                    onAction = { viewModel.refresh() }
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.posts, key = { it.postId }) { post ->
                        PostCard(
                            post = post,
                            myVote = state.votes[post.postId],
                            isBookmarked = state.bookmarks.contains(post.postId),
                            onClick = { onOpenPost(post.postId) },
                            onUpvote = { viewModel.vote(post, com.studyit.data.model.VoteType.UP) },
                            onDownvote = { viewModel.vote(post, com.studyit.data.model.VoteType.DOWN) },
                            onBookmark = { viewModel.toggleBookmark(post) },
                            onShare = {
                                shareText(
                                    context,
                                    "\"" + post.title + "\" on Studit — open the app to answer."
                                )
                            },
                            onOpenAuthor = { onOpenProfile(post.authorId) }
                        )
                    }
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.loadingMore) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                TextButton(onClick = { viewModel.loadMore() }) {
                                    Text("Load more")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
