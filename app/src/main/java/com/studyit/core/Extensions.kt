package com.studyit.core

import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

fun Timestamp?.timeAgo(): String {
    if (this == null) return "just now"
    val millis = System.currentTimeMillis() - (seconds * 1000L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val days = TimeUnit.MILLISECONDS.toDays(millis)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        days < 30 -> "${days / 7}w ago"
        days < 365 -> "${days / 30}mo ago"
        else -> "${days / 365}y ago"
    }
}

fun String.initials(): String {
    val parts = trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "S"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

fun isValidEmail(email: String): Boolean {
    val trimmed = email.trim()
    return trimmed.contains("@") &&
        trimmed.contains(".") &&
        trimmed.length >= 6 &&
        !trimmed.contains(" ")
}

fun usernameError(username: String): String? {
    val value = username.trim()
    return when {
        value.length < 3 -> "Username must be at least 3 characters"
        value.length > 20 -> "Username must be at most 20 characters"
        !value.all { it.isLetterOrDigit() || it == '_' || it == '.' } ->
            "Use only letters, numbers, dots and underscores"
        else -> null
    }
}

fun passwordError(password: String): String? = when {
    password.length < 6 -> "Password must be at least 6 characters"
    else -> null
}

fun formatPoints(points: Long): String = when {
    points >= 1_000_000 -> "${points / 100_000 / 10.0}M"
    points >= 1_000 -> "${points / 100 / 10.0}k"
    else -> points.toString()
}
