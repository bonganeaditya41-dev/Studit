package com.studyit.core

object Collections {
    const val USERS = "users"
    const val COMMUNITIES = "communities"
    const val COMMUNITY_MEMBERS = "communityMembers"
    const val POSTS = "posts"
    const val ANSWERS = "answers"
    const val COMMENTS = "comments"
    const val VOTES = "votes"
    const val BOOKMARKS = "bookmarks"
    const val NOTIFICATIONS = "notifications"
    const val REPORTS = "reports"
    const val POINTS_EVENTS = "pointsEvents"
}

object Subjects {
    val ALL = listOf(
        "Physics",
        "Chemistry",
        "Biology",
        "Mathematics",
        "Computer Science",
        "English",
        "Other"
    )
}

object AcademicLevels {
    val ALL = listOf(
        "Class 9",
        "Class 10",
        "Class 11",
        "Class 12",
        "Undergraduate",
        "Postgraduate",
        "Other"
    )
}

object PointsConfig {
    const val QUESTION_CREATED = 2
    const val QUESTION_UPVOTE = 2
    const val QUESTION_DOWNVOTE = -1
    const val ANSWER_UPVOTE = 5
    const val ANSWER_DOWNVOTE = -2
    const val ANSWER_ACCEPTED = 25
    const val DAILY_PARTICIPATION = 1
    /** Upper bound the security rules allow in a single points write. */
    const val MAX_SINGLE_WRITE = 30
}

object Defaults {
    const val FEED_PAGE_SIZE = 15L
    const val LIST_LIMIT = 50L
    const val DEFAULT_COMMUNITY = "r/StudyTips"
}
