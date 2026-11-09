package com.studyit.ui.search

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyit.ui.components.Avatar
import com.studyit.ui.components.BackButton
import com.studyit.ui.components.GlassCard
import com.studyit.ui.components.PostCard
import com.studyit.ui.components.StuditTextField
import com.studyit.ui.components.Tag

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenCommunity: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = onBack)
            StuditTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = "Search Studit",
                placeholder = "Questions, communities, students",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SearchTab.entries.forEach { tab ->
                FilterChip(
                    selected = state.tab == tab,
                    onClick = { viewModel.setTab(tab) },
                    label = { Text(tab.label) }
                )
            }
        }

        if (state.searching) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.height(22.dp))
            }
        }

        if (state.query.length < 2) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Recent searches",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.recent.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearHistory) { Text("Clear") }
                    }
                }
                state.recent.forEach { recent ->
                    TextButton(onClick = { viewModel.submit(recent) }) { Text(recent) }
                }
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (state.tab) {
                SearchTab.POSTS -> {
                    if (state.posts.isEmpty()) {
                        item { EmptyResult() }
                    }
                    items(state.posts, key = { it.postId }) { post ->
                        PostCard(
                            post = post,
                            myVote = null,
                            isBookmarked = false,
                            onClick = {
                                viewModel.submit(state.query)
                                onOpenPost(post.postId)
                            },
                            onUpvote = { },
                            onDownvote = { },
                            onBookmark = { },
                            onShare = { },
                            onOpenAuthor = { }
                        )
                    }
                }
                SearchTab.COMMUNITIES -> {
                    if (state.communities.isEmpty()) {
                        item { EmptyResult() }
                    }
                    items(state.communities, key = { it.communityId }) { community ->
                        GlassCard(onClick = { onOpenCommunity(community.communityId) }) {
                            Text(community.slug, style = MaterialTheme.typography.titleMedium)
                            Text(
                                community.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                community.memberCount.toString() + " members",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                SearchTab.USERS -> {
                    if (state.users.isEmpty()) {
                        item { EmptyResult() }
                    }
                    items(state.users, key = { it.uid }) { user ->
                        GlassCard(onClick = { onOpenUser(user.uid) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(user.photoUrl, user.initials, size = 40.dp)
                                Spacer(Modifier.height(4.dp))
                                Column(modifier = Modifier.padding(start = 10.dp)) {
                                    Text(user.displayName, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        "@" + user.username + " · " + user.rankEnum.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                SearchTab.SUBJECTS -> {
                    if (state.subjects.isEmpty()) {
                        item { EmptyResult() }
                    }
                    items(state.subjects) { subject ->
                        GlassCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Tag(subject)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Tap to search questions in this subject",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyResult() {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Text("No matches yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Try a shorter keyword or a different tab.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
