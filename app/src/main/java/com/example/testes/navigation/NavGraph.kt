package com.example.testes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testes.data.api.SessionManager
import com.example.testes.data.local.MissionsRepository
import com.example.testes.ui.screens.*
import com.example.testes.viewmodel.*

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route
) {
    val context = LocalContext.current
    val onboardingPrefs = context.getSharedPreferences("fisica_interativa_onboarding", android.content.Context.MODE_PRIVATE)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onTimeout = {
                val destination = when {
                    SessionManager.isLoggedIn -> Screen.Home.route
                    onboardingPrefs.getBoolean("seen", false) -> Screen.Login.route
                    else -> Screen.Onboarding.route
                }
                navController.navigate(destination) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
        
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    onboardingPrefs.edit().putBoolean("seen", true).apply()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate(Screen.Register.route) },
                onForgotPassword = { navController.navigate(Screen.Support.route) },
                onSupportClick = { navController.navigate(Screen.Support.route) }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onBackClick = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onDailyChallenge = { navController.navigate(Screen.DailyChallengesList.route) { launchSingleTop = true } },
                onStudyCampaign = { navController.navigate(Screen.StudyCampaign.route) { launchSingleTop = true } },
                onChatDoubt = { navController.navigate(Screen.Chat.createRoute()) { launchSingleTop = true } },
                onImprovementStats = { navController.navigate(Screen.ImprovementStats.route) { launchSingleTop = true } },
                onMissions = {
                    navController.navigate(Screen.StudyCampaign.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Screen.DailyChallenge.route) {
            val homeViewModel: HomeViewModel = viewModel()
            DailyChallengeScreen(
                viewModel = homeViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.StudyCampaign.route) {
            GalaxyScreenRoute(
                onBack = { navController.popBackStack() },
                onPlanetSelected = { planetId ->
                    navController.navigate(Screen.PlanetMap.createRoute(planetId)) { launchSingleTop = true }
                }
            )
        }

        composable(
            route = Screen.PlanetMap.route,
            arguments = listOf(navArgument("planetId") { type = NavType.StringType })
        ) { backStackEntry ->
            val planetId = backStackEntry.arguments?.getString("planetId").orEmpty()
            PlanetMapScreenRoute(
                planetId = planetId,
                onBack = { navController.popBackStack() },
                onMissionSelected = { node, planet ->
                    val route = if (planet.id == MissionsRepository.DIMENSIONAL_SUBJECT_ID)
                        Screen.MissionDetail.createRoute(node.id)
                    else
                        Screen.ModuleDetail.createRoute(node.id)
                    navController.navigate(route) { launchSingleTop = true }
                }
            )
        }

        composable(
            route = Screen.MissionDetail.route,
            arguments = listOf(navArgument("missionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val missionId = backStackEntry.arguments?.getString("missionId").orEmpty()
            MissionDetailScreenRoute(
                missionId = missionId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DailyChallengesList.route) {
            DailyChallengesScreenRoute(
                onBack = { navController.popBackStack() },
                onInstanceSelected = { instanceId ->
                    navController.navigate(Screen.DailyChallengeRun.createRoute(instanceId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Screen.DailyChallengeRun.route,
            arguments = listOf(navArgument("instanceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val instanceId = backStackEntry.arguments?.getString("instanceId").orEmpty()
            DailyChallengeRunScreenRoute(
                instanceId = instanceId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ImprovementStats.route) {
            ImprovementStatsScreen(onBackClick = { navController.popBackStack() })
        }
        
        composable(Screen.Lessons.route) {
            LessonsScreen(
                onModuleSelect = { moduleName ->
                    navController.navigate(Screen.ModuleDetail.createRoute(moduleName))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ModuleDetail.route,
            arguments = listOf(navArgument("moduleName") { type = NavType.StringType })
        ) { backStackEntry ->
            val moduleName = backStackEntry.arguments?.getString("moduleName")
            ModuleDetailScreen(
                moduleName = moduleName,
                onBackClick = { navController.popBackStack() },
                onLessonClick = { lessonId ->
                    navController.navigate(Screen.LessonDetail.createRoute(lessonId))
                }
            )
        }
        
        composable(
            route = Screen.LessonDetail.route,
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId")
            LessonDetailScreen(
                lessonId = lessonId,
                onBackClick = { navController.popBackStack() },
                onStartChat = { navController.navigate(Screen.Chat.createRoute()) }
            )
        }
        
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("action") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val action = backStackEntry.arguments?.getString("action")
            val initialRequest = if (action == "camera") AttachmentRequest.CameraNow else null
            val chatViewModel: ChatViewModel = viewModel()
            ChatScreen(
                viewModel = chatViewModel,
                onBackClick = { navController.popBackStack() },
                onOpenLesson = { lessonId ->
                    navController.navigate(Screen.LessonDetail.createRoute(lessonId))
                },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                initialAttachmentRequest = initialRequest
            )
        }
        
        composable(Screen.Profile.route) {
            val profileViewModel: ProfileViewModel = viewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                onLogout = {
                    SessionManager.clear()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSeeGeneralProgress = { navController.navigate(Screen.GeneralProgress.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onSupportClick = { navController.navigate(Screen.Support.route) },
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onAccountDeleted = {
                    SessionManager.clear()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Support.route) {
            SupportScreen(onBackClick = { navController.popBackStack() })
        }
        
        composable(Screen.GeneralProgress.route) {
            GeneralProgressScreen(
                onBackClick = { navController.popBackStack() },
                onSubjectClick = { subjectName ->
                    navController.navigate(Screen.SubjectDetail.createRoute(subjectName))
                }
            )
        }

        composable(
            route = Screen.SubjectDetail.route,
            arguments = listOf(navArgument("subjectName") { type = NavType.StringType })
        ) { backStackEntry ->
            val subjectName = backStackEntry.arguments?.getString("subjectName")
            ModuleDetailScreen( // Reusing the grid layout for lessons of the subject
                moduleName = subjectName,
                onBackClick = { navController.popBackStack() },
                onLessonClick = { lessonId ->
                    navController.navigate(Screen.LessonDetail.createRoute(lessonId))
                }
            )
        }
        
        composable(Screen.Progress.route) {
            val profileViewModel: ProfileViewModel = viewModel()
            ProgressScreen(
                viewModel = profileViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
