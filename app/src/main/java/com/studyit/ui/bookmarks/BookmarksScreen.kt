package com.studyit.ui.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import com.studyit.ui.components.BackButton
import com.studyit.ui.components.EmptyState
import com.studyit.ui.components.LoadingBox
import com.studyit.ui.components.PostCard

@Composable
fun BookmarksScreen(
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 6.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = onBack)
            Text("Saved posts", style = MaterialTheme.typography.headlineSmall)
        }

        when {
            state.loading -> LoadingBox()
            state.posts.isEmpty() -> EmptyState(
                icon = Icons.Default.Favorite,
                title = "No saved posts",
                message = "Tap the bookmark icon on any question to keep it here."
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.posts, key = { it.postId }) { post ->
                    Column {
                        PostCard(
                            post = post,
                            myVote = null,
                            isBookmarked = true,
                            onClick = { onOpenPost(post.postId) },
                            onUpvote = { },
                            onDownvote = { },
                            onBookmark = { viewModel.remove(post) },
                            onShare = { },
                            onOpenAuthor = { }
                        )
                        Row {
                            TextButton(onClick = { viewModel.remove(post) }) {
                                Text("Remove bookmark")
                            }
                        }
                    }
                }
            }
        }
    }
}
