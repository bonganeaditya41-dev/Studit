package com.studyit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.studyit.core.timeAgo
import com.studyit.data.model.Post
import com.studyit.data.model.VoteType

@Composable
fun PostCard(
    post: Post,
    myVote: VoteType?,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    onOpenAuthor: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(
                photoUrl = post.authorPhotoUrl,
                initials = post.authorName.initialsOrName(),
                size = 36.dp,
                modifier = Modifier.clip(RoundedCornerShape(50))
            )
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    post.authorName,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = (if (post.communityName.isBlank()) "r/Studit" else "r/" + post.communityName) +
                        " · " + post.createdAt.timeAgo(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (post.subject.isNotBlank()) {
                Tag(post.subject)
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            post.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        if (post.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                post.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (post.imageUrl.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }
        if (post.tags.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                post.tags.take(3).forEach { tag ->
                    Tag(
                        text = "#" + tag,
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onUpvote, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Upvote",
                    tint = if (myVote == VoteType.UP) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                (post.upvotesCount - post.downvotesCount).toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (myVote != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDownvote, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Downvote",
                    tint = if (myVote == VoteType.DOWN) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.size(6.dp))
            Icon(
                Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(4.dp))
            Text(
                post.answersCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Share",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onBookmark, modifier = Modifier.size(34.dp)) {
                Icon(
                    if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Bookmark",
                    modifier = Modifier.size(18.dp),
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun String.initialsOrName(): String {
    if (isBlank()) return "S"
    val parts = trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "S"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
