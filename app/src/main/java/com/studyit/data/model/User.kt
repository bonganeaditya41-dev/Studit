package com.studyit.data.model

import com.google.firebase.Timestamp
import com.studyit.core.initials

data class User(
    val uid: String = "",
    val fullName: String = "",
    val username: String = "",
    val usernameLower: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val academicClass: String = "",
    val totalPoints: Long = 0L,
    val rank: String = Rank.BEGINNER.label,
    val questionsCount: Long = 0L,
    val answersCount: Long = 0L,
    val acceptedAnswersCount: Long = 0L,
    val upvotesReceived: Long = 0L,
    val joinedCommunityIds: List<String> = emptyList(),
    val fcmToken: String = "",
    val isProfilePublic: Boolean = true,
    val createdAt: Timestamp? = null
) {
    val rankEnum: Rank get() = Rank.fromPoints(totalPoints.toInt())
    val displayName: String get() = if (fullName.isBlank()) username else fullName
    val initials: String get() = displayName.initials()
}

data class LeaderboardEntry(
    val user: User = User(),
    val points: Long = 0L,
    val position: Int = 0
)
