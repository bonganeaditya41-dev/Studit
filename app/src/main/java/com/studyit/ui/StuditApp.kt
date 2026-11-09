package com.studyit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.studyit.ui.auth.ForgotPasswordScreen
import com.studyit.ui.auth.LoginScreen
import com.studyit.ui.auth.SignUpScreen
import com.studyit.ui.bookmarks.BookmarksScreen
import com.studyit.ui.communities.CommunitiesScreen
import com.studyit.ui.communities.CommunityDetailScreen
import com.studyit.ui.communities.CreateCommunityScreen
import com.studyit.ui.home.HomeScreen
import com.studyit.ui.leaderboard.LeaderboardScreen
import com.studyit.ui.navigation.Routes
import com.studyit.ui.notifications.NotificationsScreen
import com.studyit.ui.post.AskQuestionScreen
import com.studyit.ui.post.PostDetailScreen
import com.studyit.ui.profile.EditProfileScreen
import com.studyit.ui.profile.ProfileScreen
import com.studyit.ui.search.SearchScreen
import com.studyit.ui.settings.SettingsScreen

@Composable
fun StuditApp(sessionViewModel: SessionViewModel = hiltViewModel()) {
    val state by sessionViewModel.authState.collectAsStateWithLifecycle()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val current = state) {
            is AuthState.Loading -> SplashScreen()
            is AuthState.SignedOut -> AuthNavigation()
            is AuthState.SignedIn -> MainNavigation(uid = current.uid)
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Studit",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Student knowledge community",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun AuthNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onSignedIn = { },
                onNavigateToSignUp = { navController.navigate(Routes.SIGN_UP) },
                onNavigateToForgot = { navController.navigate(Routes.FORGOT_PASSWORD) }
            )
        }
        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onSignedIn = { },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MainNavigation(uid: String) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topLevelRoutes = listOf(
        Routes.HOME,
        Routes.COMMUNITIES,
        Routes.ASK,
        Routes.NOTIFICATIONS,
        Routes.PROFILE
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                StuditBottomBar(
                    currentRoute = currentRoute,
                    onSelect = { route -> navigateTopLevel(navController, route) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenPost = { navController.navigate(Routes.post(it)) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onOpenLeaderboard = { navController.navigate(Routes.LEADERBOARD) },
                    onOpenProfile = { navController.navigate(Routes.user(it)) }
                )
            }
            composable(Routes.COMMUNITIES) {
                CommunitiesScreen(
                    onOpenCommunity = { navController.navigate(Routes.community(it)) },
                    onCreateCommunity = { navController.navigate(Routes.CREATE_COMMUNITY) }
                )
            }
            composable(Routes.ASK) {
                AskQuestionScreen(
                    onPublished = { postId ->
                        navController.navigate(Routes.post(postId)) {
                            popUpTo(Routes.HOME)
                        }
                    }
                )
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    onOpenPost = { navController.navigate(Routes.post(it)) }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    uid = uid,
                    isOwnProfile = true,
                    onBack = null,
                    onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                    onOpenLeaderboard = { navController.navigate(Routes.LEADERBOARD) },
                    onOpenPost = { navController.navigate(Routes.post(it)) }
                )
            }
            composable(Routes.POST, arguments = listOf(navArgument("postId") { type = NavType.StringType })) { entry ->
                val postId = entry.arguments?.getString("postId").orEmpty()
                PostDetailScreen(
                    postId = postId,
                    onBack = { navController.popBackStack() },
                    onOpenProfile = { navController.navigate(Routes.user(it)) },
                    onOpenCommunity = { navController.navigate(Routes.community(it)) }
                )
            }
            composable(Routes.COMMUNITY, arguments = listOf(navArgument("communityId") { type = NavType.StringType })) { entry ->
                val communityId = entry.arguments?.getString("communityId").orEmpty()
                CommunityDetailScreen(
                    communityId = communityId,
                    onBack = { navController.popBackStack() },
                    onOpenPost = { navController.navigate(Routes.post(it)) }
                )
            }
            composable(Routes.USER, arguments = listOf(navArgument("uid") { type = NavType.StringType })) { entry ->
                val otherUid = entry.arguments?.getString("uid").orEmpty()
                ProfileScreen(
                    uid = otherUid,
                    isOwnProfile = otherUid == uid,
                    onBack = { navController.popBackStack() },
                    onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                    onOpenLeaderboard = { navController.navigate(Routes.LEADERBOARD) },
                    onOpenPost = { navController.navigate(Routes.post(it)) }
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPost = { navController.navigate(Routes.post(it)) },
                    onOpenCommunity = { navController.navigate(Routes.community(it)) },
                    onOpenUser = { navController.navigate(Routes.user(it)) }
                )
            }
            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.BOOKMARKS) {
                BookmarksScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPost = { navController.navigate(Routes.post(it)) }
                )
            }
            composable(Routes.LEADERBOARD) {
                LeaderboardScreen(
                    onBack = { navController.popBackStack() },
                    onOpenUser = { navController.navigate(Routes.user(it)) }
                )
            }
            composable(Routes.CREATE_COMMUNITY) {
                CreateCommunityScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { communityId ->
                        navController.navigate(Routes.community(communityId)) {
                            popUpTo(Routes.COMMUNITIES)
                        }
                    }
                )
            }
        }
    }
}

private fun navigateTopLevel(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun StuditBottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomBarItem(
                icon = Icons.Default.Home,
                label = "Home",
                selected = currentRoute == Routes.HOME,
                onClick = { onSelect(Routes.HOME) }
            )
            BottomBarItem(
                icon = Icons.Default.List,
                label = "Spaces",
                selected = currentRoute == Routes.COMMUNITIES,
                onClick = { onSelect(Routes.COMMUNITIES) }
            )
            AskBarButton(
                selected = currentRoute == Routes.ASK,
                onClick = { onSelect(Routes.ASK) }
            )
            BottomBarItem(
                icon = Icons.Default.Notifications,
                label = "Alerts",
                selected = currentRoute == Routes.NOTIFICATIONS,
                onClick = { onSelect(Routes.NOTIFICATIONS) }
            )
            BottomBarItem(
                icon = Icons.Default.Person,
                label = "Profile",
                selected = currentRoute == Routes.PROFILE,
                onClick = { onSelect(Routes.PROFILE) }
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .then(Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AskBarButton(selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 8.dp,
        modifier = Modifier.size(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Ask a question",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
