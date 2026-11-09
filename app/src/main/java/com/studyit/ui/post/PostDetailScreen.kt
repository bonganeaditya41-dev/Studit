package com.studyit.ui.post

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.studyit.core.shareText
import com.studyit.core.timeAgo
import com.studyit.data.model.Answer
import com.studyit.data.model.AnswerSort
import com.studyit.data.model.ReportTargetType
import com.studyit.data.model.VoteType
import com.studyit.ui.components.Avatar
import com.studyit.ui.components.BackButton
import com.studyit.ui.components.GlassCard
import com.studyit.ui.components.PrimaryButton
import com.studyit.ui.components.StuditTextField
import com.studyit.ui.components.Tag

@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenCommunity: (String) -> Unit,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var answerText by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }
    var showReport by remember { mutableStateOf(false) }

    LaunchedEffect(postId) { viewModel.bind(postId) }
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
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
        val post = state.post
        if (post == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                BackButton(onBack = onBack)
                if (state.loading) {
                    com.studyit.ui.components.LoadingBox()
                } else {
                    com.studyit.ui.components.ErrorState("This post is no longer available")
                }
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
                    Text(
                        "Question",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showReport = true }) {
                        Icon(Icons.Default.Warning, contentDescription = "Report")
                    }
                }
            }

            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(
                            photoUrl = post.authorPhotoUrl,
                            initials = post.authorName.take(1).uppercase(),
                            size = 40.dp
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                post.authorName,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            )
                            Text(
                                "r/" + post.communityName + " · " + post.createdAt.timeAgo(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onOpenProfile(post.authorId) }) { Text("View") }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        post.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(post.description, style = MaterialTheme.typography.bodyLarge)

                    if (post.imageUrl.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }

                    if (post.tags.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            post.tags.forEach { tag ->
                                Tag(
                                    "#" + tag,
                                    container = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.votePost(VoteType.UP) }) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Upvote",
                                tint = if (state.myVote == VoteType.UP) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            (post.upvotesCount - post.downvotesCount).toString(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { viewModel.votePost(VoteType.DOWN) }) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Downvote",
                                tint = if (state.myVote == VoteType.DOWN) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = viewModel::toggleBookmark) {
                            Icon(
                                if (state.bookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Bookmark",
                                tint = if (state.bookmarked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            shareText(context, post.title + " — asked on Studit")
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                    TextButton(onClick = { onOpenCommunity(post.communityId) }) {
                        Text("Open r/" + post.communityName)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        post.answersCount.toString() + " answers",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    AnswerSort.entries.forEach { sort ->
                        FilterChip(
                            selected = state.answerSort == sort,
                            onClick = { viewModel.setAnswerSort(sort) },
                            label = { Text(sort.label) },
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }

            if (state.sortedAnswers.isEmpty()) {
                item {
                    GlassCard {
                        Text(
                            "No answers yet. Be the first to help.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(state.sortedAnswers, key = { it.answerId }) { answer ->
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(
                            photoUrl = answer.authorPhotoUrl,
                            initials = answer.authorName.take(1).uppercase(),
                            size = 34.dp
                        )
                        Spacer(Modifier.size(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(answer.authorName, style = MaterialTheme.typography.labelLarge)
                            Text(
                                answer.createdAt.timeAgo(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (answer.isAccepted) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    "Accepted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(answer.content, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.voteAnswer(answer, VoteType.UP) }) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Upvote answer",
                                tint = if (state.answerVotes[answer.answerId] == VoteType.UP)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text((answer.upvotesCount - answer.downvotesCount).toString())
                        IconButton(onClick = { viewModel.voteAnswer(answer, VoteType.DOWN) }) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Downvote answer",
                                tint = if (state.answerVotes[answer.answerId] == VoteType.DOWN)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (post.authorId == state.currentUser?.uid && !answer.isAccepted) {
                            TextButton(onClick = { viewModel.acceptAnswer(answer) }) {
                                Text("Accept")
                            }
                        }
                        if (answer.authorId == state.currentUser?.uid) {
                            TextButton(onClick = { viewModel.deleteAnswer(answer) }) {
                                Text("Delete")
                            }
                        }
                        TextButton(onClick = {
                            replyTarget = if (replyTarget == answer.answerId) null else answer.answerId
                        }) { Text("Reply") }
                    }

                    if (replyTarget == answer.answerId) {
                        Spacer(Modifier.height(6.dp))
                        StuditTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            label = "Reply",
                            singleLine = false,
                            minLines = 2
                        )
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = {
                                viewModel.addComment(answer.answerId, "", replyText)
                                replyText = ""
                                replyTarget = null
                            }) { Text("Send reply") }
                        }
                    }

                    val comments = state.commentsFor(answer.answerId)
                    comments.forEach { comment ->
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    comment.authorName + " · " + comment.createdAt.timeAgo(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(comment.content, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (comment.authorId == state.currentUser?.uid) {
                                TextButton(onClick = { viewModel.deleteComment(comment) }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }

            item {
                GlassCard {
                    Text("Your answer", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    StuditTextField(
                        value = answerText,
                        onValueChange = { answerText = it },
                        label = "Explain your reasoning",
                        singleLine = false,
                        minLines = 4
                    )
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton(
                        text = "Post answer",
                        loading = state.sending,
                        enabled = answerText.trim().length >= 5,
                        onClick = {
                            viewModel.submitAnswer(answerText)
                            answerText = ""
                        }
                    )
                }
            }
        }

        if (showReport) {
            AlertDialog(
                onDismissRequest = { showReport = false },
                title = { Text("Report this question") },
                text = { Text("Spam, abuse or off-topic content will be reviewed by moderators.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.report(post.postId, ReportTargetType.POST, "Reported by user")
                        showReport = false
                    }) { Text("Send report") }
                },
                dismissButton = {
                    TextButton(onClick = { showReport = false }) { Text("Cancel") }
                }
            )
        }
    }
}
