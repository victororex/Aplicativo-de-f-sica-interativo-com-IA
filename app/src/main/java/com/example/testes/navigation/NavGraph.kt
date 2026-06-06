package com.example.testes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testes.data.api.SessionManager
import com.example.testes.ui.screens.*
import com.example.testes.viewmodel.*

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onTimeout = {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
        
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
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
                onRegisterClick = { navController.navigate(Screen.Register.route) }
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
                onDailyChallenge = { navController.navigate(Screen.DailyChallenge.route) },
                onStudyCampaign = { navController.navigate(Screen.StudyCampaign.route) },
                onChatDoubt = { navController.navigate(Screen.Chat.route) },
                onImprovementStats = { navController.navigate(Screen.ImprovementStats.route) },
                onBackClick = { 
                    if (!navController.popBackStack()) {
                        // If no backstack, maybe go to a default or do nothing
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
            StudyCampaignScreen(
                onBackClick = { navController.popBackStack() },
                onSubjectClick = { subjectId ->
                    navController.navigate(Screen.ModuleDetail.createRoute(subjectId))
                }
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
                onStartChat = { navController.navigate(Screen.Chat.route) }
            )
        }
        
        composable(Screen.Chat.route) {
            val chatViewModel: ChatViewModel = viewModel()
            ChatScreen(
                viewModel = chatViewModel,
                onBackClick = { navController.popBackStack() }
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
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.AvatarCustomization.route) {
            AvatarCustomizationScreen(onBackClick = { navController.popBackStack() })
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
