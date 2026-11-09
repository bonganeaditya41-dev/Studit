package com.studyit.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val FORGOT_PASSWORD = "forgot"

    const val HOME = "home"
    const val COMMUNITIES = "communities"
    const val ASK = "ask"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"

    const val POST = "post/{postId}"
    const val COMMUNITY = "community/{communityId}"
    const val USER = "user/{uid}"
    const val SEARCH = "search"
    const val EDIT_PROFILE = "editProfile"
    const val SETTINGS = "settings"
    const val BOOKMARKS = "bookmarks"
    const val LEADERBOARD = "leaderboard"
    const val CREATE_COMMUNITY = "createCommunity"

    fun post(postId: String) = "post/$postId"
    fun community(communityId: String) = "community/$communityId"
    fun user(uid: String) = "user/$uid"
}
