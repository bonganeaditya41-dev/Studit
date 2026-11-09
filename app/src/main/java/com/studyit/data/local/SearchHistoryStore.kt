package com.studyit.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("studit_search", Context.MODE_PRIVATE)

    fun recent(): List<String> =
        prefs.getString("recent", "").orEmpty()
            .split("\n")
            .filter { it.isNotBlank() }

    fun add(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        val updated = (listOf(clean) + recent().filterNot { it.equals(clean, true) }).take(8)
        prefs.edit().putString("recent", updated.joinToString("\n")).apply()
    }

    fun clear() = prefs.edit().remove("recent").apply()
}
