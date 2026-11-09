package com.studyit.ui.post

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.studyit.core.AcademicLevels
import com.studyit.core.Subjects
import com.studyit.ui.components.GlassCard
import com.studyit.ui.components.PrimaryButton
import com.studyit.ui.components.StuditDropdown
import com.studyit.ui.components.StuditTextField

@Composable
fun AskQuestionScreen(
    onPublished: (String) -> Unit,
    viewModel: AskQuestionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> viewModel.onImagePicked(uri) }

    LaunchedEffect(state.publishedPostId) {
        state.publishedPostId?.let { onPublished(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Ask a question", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Clear doubts get clear answers — add context, your attempt and what confuses you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        GlassCard {
            StuditTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = "Question title",
                placeholder = "e.g. Why is angular momentum conserved here?",
                maxLength = 120
            )
            Spacer(Modifier.height(12.dp))
            StuditTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = "Describe your doubt",
                placeholder = "Share the problem, your attempt and where you got stuck",
                singleLine = false,
                minLines = 6
            )
            Spacer(Modifier.height(12.dp))
            StuditDropdown(
                label = "Subject",
                options = Subjects.ALL,
                selected = state.subject,
                onSelect = viewModel::onSubjectChange
            )
            Spacer(Modifier.height(12.dp))
            StuditDropdown(
                label = "Academic level",
                options = AcademicLevels.ALL,
                selected = state.academicClass,
                onSelect = viewModel::onClassChange,
                placeholder = "Select level"
            )
            Spacer(Modifier.height(12.dp))
            if (state.communities.isEmpty()) {
                Text(
                    "Loading communities…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                StuditDropdown(
                    label = "Community",
                    options = state.communities.map { it.slug },
                    selected = if (state.communityName.isBlank()) "" else "r/" + state.communityName,
                    onSelect = { slug ->
                        state.communities.firstOrNull { it.slug == slug }?.let(viewModel::onCommunityChange)
                    },
                    placeholder = "Choose a community"
                )
            }
            Spacer(Modifier.height(12.dp))
            StuditTextField(
                value = state.tags,
                onValueChange = viewModel::onTagsChange,
                label = "Tags",
                placeholder = "kinematics, jee, chapter-5 (comma separated)"
            )

            Spacer(Modifier.height(14.dp))
            if (state.imageUri != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = state.imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        "Image attached",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::clearImage) {
                        Icon(Icons.Default.Close, contentDescription = "Remove image")
                    }
                }
            } else {
                TextButton(onClick = { imagePicker.launch("image/*") }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Attach an image")
                }
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                state.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (state.draftSaved) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Draft saved on this device.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            text = "Publish question",
            loading = state.publishing,
            enabled = state.canPublish,
            onClick = viewModel::publish
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = viewModel::saveDraft) { Text("Save as draft") }
        }
        Spacer(Modifier.height(20.dp))
    }
}
