package com.studyit.ui.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyit.data.model.LeaderboardRange
import com.studyit.ui.components.Avatar
import com.studyit.ui.components.BackButton
import com.studyit.ui.components.ErrorState
import com.studyit.ui.components.GlassCard
import com.studyit.ui.components.LoadingBox

@Composable
fun LeaderboardScreen(
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            BackButton(onBack = onBack)
            Text("Leaderboard", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "Points are earned for questions, answers and accepted solutions.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LeaderboardRange.entries.forEach { range ->
                FilterChip(
                    selected = state.range == range,
                    onClick = { viewModel.setRange(range) },
                    label = { Text(range.label) }
                )
            }
        }

        when {
            state.loading -> LoadingBox()
            state.error != null -> ErrorState(state.error.orEmpty()) { viewModel.setRange(state.range) }
            state.entries.isEmpty() -> Text(
                "No points earned in this period yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.entries, key = { it.user.uid }) { entry ->
                    GlassCard(onClick = { onOpenUser(entry.user.uid) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "#" + entry.position,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Avatar(entry.user.photoUrl, entry.user.initials, size = 38.dp)
                            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                                Text(entry.user.displayName, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "@" + entry.user.username + " · " + entry.user.rankEnum.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                entry.points.toString() + " pts",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.padding(bottom = 12.dp))
    }
}
