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
    object Chat : Screen("chat?action={action}") {
        fun createRoute(action: String? = null): String =
            if (action == null) "chat" else "chat?action=$action"
    }
    object Profile : Screen("profile")
    object Progress : Screen("progress")
    object DailyChallenge : Screen("daily_challenge")
    object StudyCampaign : Screen("study_campaign")
    object PlanetMap : Screen("planet_map/{planetId}") {
        fun createRoute(planetId: String) = "planet_map/${Uri.encode(planetId)}"
    }
    object MissionQuiz : Screen("mission_quiz/{nodeId}") {
        fun createRoute(nodeId: String) = "mission_quiz/${Uri.encode(nodeId)}"
    }
    object MissionDetail : Screen("mission_detail/{missionId}") {
        fun createRoute(missionId: String) = "mission_detail/${Uri.encode(missionId)}"
    }
    object DailyChallengesList : Screen("daily_challenges_list")
    object DailyChallengeRun : Screen("daily_challenge_run/{instanceId}") {
        fun createRoute(instanceId: String) = "daily_challenge_run/${Uri.encode(instanceId)}"
    }
    object ImprovementStats : Screen("improvement_stats")
    object ModuleDetail : Screen("module_detail/{moduleName}") {
        fun createRoute(moduleName: String) = "module_detail/${Uri.encode(moduleName)}"
    }
    object GeneralProgress : Screen("general_progress")
    object SubjectDetail : Screen("subject_detail/{subjectName}") {
        fun createRoute(subjectName: String) = "subject_detail/${Uri.encode(subjectName)}"
    }
    object Settings : Screen("settings")
    object Register : Screen("register")
    object Support : Screen("support")
}
