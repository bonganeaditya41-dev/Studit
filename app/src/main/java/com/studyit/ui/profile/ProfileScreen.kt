package com.studyit.ui.profile

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyit.data.model.Rank
import com.studyit.ui.components.Avatar
import com.studyit.ui.components.BackButton
import com.studyit.ui.components.GlassCard
import com.studyit.ui.components.LoadingBox
import com.studyit.ui.components.PostCard
import com.studyit.ui.components.StatPill
import com.studyit.ui.components.Tag

@Composable
fun ProfileScreen(
    uid: String,
    isOwnProfile: Boolean,
    onBack: (() -> Unit)?,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenPost: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(uid) { viewModel.bind(uid) }

    if (state.loading) {
        LoadingBox()
        return
    }

    val user = state.user
    if (user == null) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            if (onBack != null) BackButton(onBack = onBack)
            Text(
                state.error ?: "Profile unavailable",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    val nextRank = Rank.next(user.totalPoints.toInt())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    BackButton(onBack = onBack)
                }
                Spacer(Modifier.weight(1f))
                if (isOwnProfile) {
                    IconButton(onClick = onOpenLeaderboard) {
                        Icon(Icons.Default.Star, contentDescription = "Leaderboard")
                    }
                    IconButton(onClick = onOpenBookmarks) {
                        Icon(Icons.Default.Favorite, contentDescription = "Bookmarks")
                    }
                    IconButton(onClick = onEditProfile) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit profile")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }
        }

        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(
                        photoUrl = user.photoUrl,
                        initials = user.initials,
                        size = 72.dp
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(
                            user.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "@" + user.username,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Tag(user.rankEnum.label)
                            if (user.academicClass.isNotBlank()) {
                                Tag(
                                    user.academicClass,
                                    container = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (user.bio.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(user.bio, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Points", user.totalPoints.toString(), Modifier.weight(1f))
                    StatPill("Questions", user.questionsCount.toString(), Modifier.weight(1f))
                    StatPill("Answers", user.answersCount.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Accepted", user.acceptedAnswersCount.toString(), Modifier.weight(1f))
                    StatPill("Upvotes", user.upvotesReceived.toString(), Modifier.weight(1f))
                    StatPill("Rank", user.rankEnum.label, Modifier.weight(1f))
                }

                if (nextRank != null) {
                    Spacer(Modifier.height(12.dp))
                    val span = (nextRank.minPoints - user.rankEnum.minPoints).toFloat()
                    val done = (user.totalPoints.toFloat() - user.rankEnum.minPoints).coerceAtLeast(0f)
                    Text(
                        (nextRank.minPoints - user.totalPoints).toString() + " points to " + nextRank.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (done / span).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                }
            }
        }

        item {
            Text(
                "Questions (" + state.posts.size + ")",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (state.posts.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        "No questions yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(state.posts, key = { it.postId }) { post ->
            PostCard(
                post = post,
                myVote = null,
                isBookmarked = false,
                onClick = { onOpenPost(post.postId) },
                onUpvote = { },
                onDownvote = { },
                onBookmark = { },
                onShare = { },
                onOpenAuthor = { }
            )
        }
    }
}
