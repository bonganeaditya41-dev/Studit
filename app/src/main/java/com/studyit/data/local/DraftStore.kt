package com.studyit.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class QuestionDraft(
    val title: String = "",
    val description: String = "",
    val subject: String = "",
    val academicClass: String = "",
    val communityId: String = "",
    val tags: String = ""
) {
    val isEmpty: Boolean
        get() = title.isBlank() && description.isBlank()
}

@Singleton
class DraftStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("studit_draft", Context.MODE_PRIVATE)

    fun save(draft: QuestionDraft) {
        prefs.edit()
            .putString("title", draft.title)
            .putString("description", draft.description)
            .putString("subject", draft.subject)
            .putString("academicClass", draft.academicClass)
            .putString("communityId", draft.communityId)
            .putString("tags", draft.tags)
            .apply()
    }

    fun load(): QuestionDraft = QuestionDraft(
        title = prefs.getString("title", "").orEmpty(),
        description = prefs.getString("description", "").orEmpty(),
        subject = prefs.getString("subject", "").orEmpty(),
        academicClass = prefs.getString("academicClass", "").orEmpty(),
        communityId = prefs.getString("communityId", "").orEmpty(),
        tags = prefs.getString("tags", "").orEmpty()
    )

    fun clear() = prefs.edit().clear().apply()
}
