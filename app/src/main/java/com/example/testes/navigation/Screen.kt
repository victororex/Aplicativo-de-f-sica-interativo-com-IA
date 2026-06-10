package com.example.testes.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Home : Screen("home")
    object Lessons : Screen("lessons")
    object LessonDetail : Screen("lesson_detail/{lessonId}") {
        fun createRoute(lessonId: String) = "lesson_detail/${Uri.encode(lessonId)}"
    }
    object Chat : Screen("chat")
    object Profile : Screen("profile")
    object Progress : Screen("progress")
    object DailyChallenge : Screen("daily_challenge")
    object StudyCampaign : Screen("study_campaign")
    object ImprovementStats : Screen("improvement_stats")
    object ModuleDetail : Screen("module_detail/{moduleName}") {
        fun createRoute(moduleName: String) = "module_detail/${Uri.encode(moduleName)}"
    }
    object GeneralProgress : Screen("general_progress")
    object SubjectDetail : Screen("subject_detail/{subjectName}") {
        fun createRoute(subjectName: String) = "subject_detail/${Uri.encode(subjectName)}"
    }
    object Settings : Screen("settings")
    object AvatarCustomization : Screen("avatar_customization")
    object Register : Screen("register")
    object Support : Screen("support")
}
