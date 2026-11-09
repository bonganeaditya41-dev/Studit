package com.studyit.data.model

import com.google.firebase.Timestamp

data class Community(
    val communityId: String = "",
    val name: String = "",
    val nameLower: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val creatorId: String = "",
    val memberCount: Long = 0L,
    val rules: List<String> = emptyList(),
    val createdAt: Timestamp? = null
) {
    val slug: String get() = "r/$name"
}

data class CommunityMember(
    val membershipId: String = "",
    val communityId: String = "",
    val uid: String = "",
    val role: String = "member",
    val joinedAt: Timestamp? = null
)
