package com.studyit.data.model

enum class VoteType { UP, DOWN }

enum class VoteTargetType(val value: String) {
    POST("POST"),
    ANSWER("ANSWER")
}

enum class FeedSort(val label: String) {
    LATEST("Latest"),
    POPULAR("Popular"),
    TRENDING("Trending"),
    UNANSWERED("Unanswered")
}

enum class AnswerSort(val label: String) {
    NEWEST("Newest"),
    TOP("Highest voted")
}

enum class NotificationType { ANSWER, REPLY, UPVOTE, ACCEPTED, MENTION, ANNOUNCEMENT }

enum class ReportTargetType { POST, ANSWER, USER }

enum class LeaderboardRange(val label: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    ALL_TIME("All time")
}

enum class Rank(val label: String, val minPoints: Int) {
    BEGINNER("Beginner", 0),
    LEARNER("Learner", 50),
    CONTRIBUTOR("Contributor", 150),
    SCHOLAR("Scholar", 400),
    EXPERT("Expert", 1000),
    MASTER_SCHOLAR("Master Scholar", 2500);

    companion object {
        fun fromPoints(points: Int): Rank =
            entries.lastOrNull { points >= it.minPoints } ?: BEGINNER

        fun next(points: Int): Rank? =
            entries.firstOrNull { points < it.minPoints }
    }
}
