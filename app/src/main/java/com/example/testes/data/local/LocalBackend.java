package com.example.testes.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class LocalBackend {
    private static final String PREFS = "fisica_dimensional_demo";
    private static final String SUBJECT_ID = "analise-dimensional";
    private static final String SUBJECT_NAME = "An\u00e1lise Dimensional";
    private static SharedPreferences prefs;

    private LocalBackend() {
    }

    public static void init(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            if (!prefs.contains("users")) {
                seedInitialUser();
            } else {
                repairUserStorage();
            }
            migrateStorage();
        } catch (JSONException error) {
            seedInitialUser();
        }
    }

    public static JSONObject register(String name, String email, String password) throws JSONException {
        ensure();
        String cleanEmail = cleanEmail(email);
        String cleanPassword = password == null ? "" : password.trim();
        if (!looksLikeEmail(cleanEmail)) {
            throw new IllegalArgumentException("Informe um e-mail valido.");
        }
        if (cleanPassword.length() < 6) {
            throw new IllegalArgumentException("Use uma senha com pelo menos 6 caracteres.");
        }

        JSONArray users = users();
        if (findUserByEmail(users, cleanEmail) != null) {
            throw new IllegalArgumentException("Este e-mail ja possui conta.");
        }

        int id = prefs.getInt("next_user_id", 1);
        JSONObject user = userJson(
                id,
                name == null || name.trim().isEmpty() ? "Aluno" : name.trim(),
                cleanEmail,
                cleanPassword,
                null,
                false,
                true
        );
        users.put(user);
        boolean saved = prefs.edit()
                .putString("users", users.toString())
                .putInt("next_user_id", id + 1)
                .commit();
        if (!saved) {
            throw new IllegalStateException("Nao foi possivel salvar sua conta.");
        }
        clearLearningState(id);
        return authResponse(user);
    }

    public static JSONObject login(String email, String password) throws JSONException {
        ensure();
        JSONObject user = findUserByEmail(users(), cleanEmail(email));
        if (user == null || !user.optString("password").equals(password)) {
            throw new IllegalArgumentException("E-mail ou senha incorretos.");
        }
        return authResponse(user);
    }

    public static void resetPassword(String email, String newPassword) throws JSONException {
        ensure();
        String cleanEmail = cleanEmail(email);
        String cleanPassword = newPassword == null ? "" : newPassword.trim();
        if (!looksLikeEmail(cleanEmail)) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
        if (cleanPassword.length() < 6) {
            throw new IllegalArgumentException("Use uma senha com pelo menos 6 caracteres.");
        }

        JSONArray users = users();
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (cleanEmail.equalsIgnoreCase(user.optString("email"))) {
                user.put("password", cleanPassword);
                prefs.edit().putString("users", users.toString()).apply();
                return;
            }
        }
        throw new IllegalArgumentException("Não encontramos uma conta com este e-mail.");
    }

    public static JSONObject currentUser(String token) throws JSONException {
        JSONObject user = userFromToken(token);
        if (user == null) {
            throw new IllegalArgumentException("Sessao expirada.");
        }
        return publicUser(user);
    }

    public static JSONObject updateCurrentUser(
            String token,
            String name,
            String email,
            String phone,
            boolean privateAccount,
            boolean notificationsEnabled
    ) throws JSONException {
        int userId = userIdFromToken(token);
        String cleanEmail = cleanEmail(email);
        if (!looksLikeEmail(cleanEmail)) {
            throw new IllegalArgumentException("Informe um e-mail valido.");
        }
        JSONArray users = users();

        JSONObject owner = findUserByEmail(users, cleanEmail);
        if (owner != null && owner.optInt("id") != userId) {
            throw new IllegalArgumentException("Este e-mail ja esta em uso.");
        }

        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (user.optInt("id") == userId) {
                user.put("name", name == null || name.trim().isEmpty() ? "Aluno" : name.trim());
                user.put("email", cleanEmail);
                user.put("phone", phone == null || phone.trim().isEmpty() ? JSONObject.NULL : phone.trim());
                user.put("private_account", privateAccount);
                user.put("notifications_enabled", notificationsEnabled);
                prefs.edit().putString("users", users.toString()).apply();
                return publicUser(user);
            }
        }
        throw new IllegalArgumentException("Usuario nao encontrado.");
    }

    public static void deleteCurrentUser(String token) throws JSONException {
        int userId = userIdFromToken(token);
        JSONArray users = users();
        JSONArray remaining = new JSONArray();
        boolean found = false;

        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (user.optInt("id") == userId) {
                found = true;
            } else {
                remaining.put(user);
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Usuario nao encontrado.");
        }

        clearLearningState(userId);
        prefs.edit()
                .putString("users", remaining.toString())
                .apply();
    }

    public static JSONArray subjects(String token) throws JSONException {
        int total = lessonsData().length();
        int completed = completedLessons(token).length();
        JSONArray subjects = new JSONArray();
        subjects.put(new JSONObject()
                .put("id", SUBJECT_ID)
                .put("name", SUBJECT_NAME)
                .put("description", "Aprenda a verificar formulas usando grandezas, unidades e dimensoes.")
                .put("exam_focus", "Trilha completa de Analise Dimensional")
                .put("total_lessons", total)
                .put("completed_lessons", completed)
                .put("progress", total == 0 ? 0.0 : (double) completed / total)
                .put("is_completed", completed >= total && total > 0));
        return subjects;
    }

    public static JSONArray lessons(String token, String subjectId) throws JSONException {
        JSONArray source = lessonsData();
        JSONArray completed = completedLessons(token);
        JSONArray result = new JSONArray();
        for (int i = 0; i < source.length(); i++) {
            JSONObject lesson = source.getJSONObject(i);
            result.put(lessonSummary(lesson, contains(completed, lesson.getString("id"))));
        }
        return result;
    }

    public static JSONObject lesson(String token, String lessonId) throws JSONException {
        int userId = userIdFromToken(token);
        JSONArray source = lessonsData();
        JSONArray completed = completedLessons(token);
        for (int i = 0; i < source.length(); i++) {
            JSONObject lesson = source.getJSONObject(i);
            if (lessonId.equals(lesson.optString("id"))) {
                recordEvent(userId, "lesson_opened", lesson.optString("id"), JSONObject.NULL, JSONObject.NULL, JSONObject.NULL, 60);
                JSONObject stats = stats(userId);
                stats.put("last_lesson_title", lesson.optString("title"));
                prefs.edit().putString(key(userId, "stats"), stats.toString()).apply();
                return lessonDetail(lesson, contains(completed, lessonId));
            }
        }
        throw new IllegalArgumentException("Aula nao encontrada.");
    }

    public static JSONObject completeLesson(String token, String lessonId) throws JSONException {
        int userId = userIdFromToken(token);
        JSONArray completed = completedLessons(token);
        if (!contains(completed, lessonId)) {
            completed.put(lessonId);
            addStats(userId, 0, 0, 0, 0, 210);
            recordEvent(userId, "lesson_completed", lessonId, JSONObject.NULL, JSONObject.NULL, JSONObject.NULL, 210);
        }
        prefs.edit().putString(key(userId, "completed_lessons"), completed.toString()).apply();
        emitAppEvent("LessonCompleted");
        return progressSummary(token);
    }

    public static JSONObject progressSummary(String token) throws JSONException {
        int userId = userIdFromToken(token);
        JSONArray completed = completedLessons(token);
        int trackTotal = dimensionalTrackTotal();
        double overall;
        if (trackTotal > 0) {
            overall = (double) dimensionalTrackCompleted(token) / trackTotal;
        } else {
            int totalLessons = lessonsData().length();
            overall = totalLessons == 0 ? 0.0 : (double) completed.length() / totalLessons;
        }
        return new JSONObject()
                .put("user_id", userId)
                .put("completed_lessons", completed)
                .put("current_module", SUBJECT_NAME)
                .put("overall_completion", Math.min(1.0, overall));
    }

    public static JSONArray dailyChallenge() throws JSONException {
        JSONArray questions = new JSONArray();
        questions.put(question(
                "daily-1",
                "Qual e a dimensao da velocidade?",
                new String[]{"[L][T]^-1", "[L][T]", "[M][L]^-1", "[T][L]^-1"},
                0,
                "Velocidade e distancia dividida por tempo: [v] = [L]/[T] = [L][T]^-1.",
                "Facil"
        ));
        questions.put(question(
                "daily-2",
                "Pela relacao F = m.a, qual e a dimensao da forca?",
                new String[]{"[M][L][T]^-2", "[M][T]^2", "[L][T]^-1", "[M][L]^2"},
                0,
                "Massa e [M]. Aceleracao e [L][T]^-2. Multiplicando: [F] = [M][L][T]^-2.",
                "Facil"
        ));
        questions.put(question(
                "daily-3",
                "Se uma formula soma metro com segundo, o que isso indica?",
                new String[]{"A formula esta incoerente", "A formula esta sempre certa", "A unidade vira joule", "Basta mudar o numero"},
                0,
                "So podemos somar grandezas com a mesma dimensao fisica.",
                "Medio"
        ));
        questions.put(question(
                "daily-4",
                "A dimensao de energia pode aparecer como:",
                new String[]{"[M][L]^2[T]^-2", "[M][L][T]", "[L]^2[T]^2", "[M][T]^-1"},
                0,
                "Em E = m.v^2, temos [M]([L][T]^-1)^2 = [M][L]^2[T]^-2.",
                "Medio"
        ));
        questions.put(question(
                "daily-5",
                "Qual alternativa combina com potencia, isto e, energia por tempo?",
                new String[]{"[M][L]^2[T]^-3", "[M][L]^2[T]^-1", "[M][L][T]^-2", "[L][T]^-3"},
                0,
                "Potencia e energia dividida por tempo. Entao [P] = [M][L]^2[T]^-2 / [T] = [M][L]^2[T]^-3.",
                "Medio"
        ));
        questions.put(question(
                "daily-6",
                "Um numero como 2, 1/2 ou pi muda a dimensao da formula?",
                new String[]{"Nao", "Sim, vira massa", "Sim, vira tempo", "So quando tem unidade"},
                0,
                "Numeros puros nao carregam unidade. Eles podem mudar o valor, mas nao a dimensao.",
                "Facil"
        ));
        return questions;
    }

    public static JSONObject dailyStatus(String token) throws JSONException {
        int userId = userIdFromToken(token);
        JSONObject attempt = jsonObject(key(userId, "daily_attempt"), null);
        if (attempt == null || !today().equals(attempt.optString("date"))) {
            return new JSONObject().put("completed_today", false);
        }

        int score = attempt.optInt("score");
        int total = attempt.optInt("total");
        return new JSONObject()
                .put("completed_today", true)
                .put("score", score)
                .put("total", total)
                .put("accuracy_rate", total == 0 ? 0 : Math.round(score * 100f / total))
                .put("completed_at", attempt.optString("date"));
    }

    public static boolean shouldShowDailyChallengePrompt(String token) {
        try {
            int userId = userIdFromToken(token);
            String dismissedDate = prefs.getString(
                    key(userId, "daily_prompt_dismissed_date"),
                    ""
            );
            return !today().equals(dismissedDate);
        } catch (RuntimeException error) {
            return false;
        }
    }

    public static void dismissDailyChallengePrompt(String token) {
        try {
            int userId = userIdFromToken(token);
            prefs.edit()
                    .putString(key(userId, "daily_prompt_dismissed_date"), today())
                    .apply();
        } catch (RuntimeException ignored) {
            // The in-memory popup still closes when there is no valid session.
        }
    }

    public static void recordDailyChallengeStarted(String token) throws JSONException {
        int userId = userIdFromToken(token);
        recordEvent(userId, "daily_challenge_started", JSONObject.NULL, "daily", JSONObject.NULL, JSONObject.NULL, 0);
    }

    public static JSONObject submitDaily(String token, int score, int total) throws JSONException {
        int userId = userIdFromToken(token);
        JSONObject existing = jsonObject(key(userId, "daily_attempt"), null);
        if (existing != null && today().equals(existing.optString("date"))) {
            return quizResult(existing.optInt("score"), existing.optInt("total"));
        }

        JSONObject attempt = new JSONObject()
                .put("date", today())
                .put("score", score)
                .put("total", total);
        prefs.edit().putString(key(userId, "daily_attempt"), attempt.toString()).apply();
        addStats(userId, total, score, 0, 0, 180);
        recordEvent(userId, "daily_challenge_completed", JSONObject.NULL, "daily", score >= total, "misto", 180);
        emitAppEvent("DailyChallengeSubmitted");
        return quizResult(score, total);
    }

    public static JSONArray campaign(String token) throws JSONException {
        int userId = userIdFromToken(token);
        JSONArray stages = campaignData();
        JSONArray result = new JSONArray();
        boolean previousComplete = true;

        for (int i = 0; i < stages.length(); i++) {
            JSONObject stage = stages.getJSONObject(i);
            JSONObject saved = jsonObject(key(userId, "campaign_" + stage.optString("id")), null);
            int total = stage.getJSONArray("exercises").length();
            int score = saved == null ? 0 : saved.optInt("score", 0);
            double progress = total == 0 ? 0.0 : (double) score / total;
            boolean unlocked = i == 0 || previousComplete || progress > 0.0;
            result.put(new JSONObject(stage.toString())
                    .put("progress", progress)
                    .put("is_unlocked", unlocked)
                    .put("lock_reason", unlocked ? JSONObject.NULL : "Conclua a etapa anterior."));
            previousComplete = progress >= 1.0;
        }

        return result;
    }

    public static JSONObject submitCampaign(String token, String nodeId, int score, int total) throws JSONException {
        int userId = userIdFromToken(token);
        JSONObject previous = jsonObject(key(userId, "campaign_" + nodeId), null);
        int previousScore = previous == null ? 0 : previous.optInt("score", 0);
        int previousTotal = previous == null ? total : previous.optInt("total", total);
        boolean wasComplete = previousTotal > 0 && previousScore >= previousTotal;
        boolean isComplete = total > 0 && score >= total;

        JSONObject saved = new JSONObject().put("score", score).put("total", total);
        prefs.edit().putString(key(userId, "campaign_" + nodeId), saved.toString()).apply();
        addStats(userId, total, score, !wasComplete && isComplete ? 1 : 0, 0, 240);
        recordEvent(userId, "campaign_stage_completed", JSONObject.NULL, nodeId, isComplete, "campanha", 240);
        emitAppEvent("CampaignStageSubmitted");
        return quizResult(score, total);
    }

    public static void recordDailyAnswer(
            String token,
            String questionId,
            String topic,
            String selectedAnswer,
            String correctAnswer,
            boolean isCorrect,
            String difficulty,
            int responseTimeSeconds
    ) throws JSONException {
        int userId = userIdFromToken(token);
        recordEventWithTopic(
                userId,
                "exercise_answered",
                topic == null || topic.trim().isEmpty() ? SUBJECT_NAME : topic.trim(),
                JSONObject.NULL,
                questionId,
                isCorrect,
                difficulty,
                Math.max(0, responseTimeSeconds),
                selectedAnswer,
                correctAnswer
        );
    }

    public static void recordCampaignAnswer(
            String token,
            String questionId,
            String topic,
            boolean isCorrect,
            String difficulty,
            int responseTimeSeconds
    ) throws JSONException {
        int userId = userIdFromToken(token);
        recordEventWithTopic(
                userId,
                "exercise_answered",
                topic == null || topic.trim().isEmpty() ? SUBJECT_NAME : topic.trim(),
                JSONObject.NULL,
                questionId,
                isCorrect,
                difficulty,
                Math.max(0, responseTimeSeconds)
        );
    }

    public static void recordTrackMissionAnswer(
            String token,
            String questionId,
            String topic,
            boolean isCorrect,
            int responseTimeSeconds
    ) throws JSONException {
        int userId = userIdFromToken(token);
        recordEventWithTopic(
                userId,
                "exercise_answered",
                topic == null || topic.trim().isEmpty() ? SUBJECT_NAME : topic.trim(),
                JSONObject.NULL,
                questionId,
                isCorrect,
                "Trilha",
                Math.max(0, responseTimeSeconds)
        );
        addStats(userId, 1, isCorrect ? 1 : 0, 0, 0, 0);
        emitAppEvent("TrackMissionAnswerRecorded");
    }

    public static JSONObject analyticsSnapshot(String token) throws JSONException {
        int userId = userIdFromToken(token);
        JSONArray events = jsonArray(key(userId, "learning_events"));
        JSONArray legacyChatHistory = chatHistory(token);
        int recordedChatQuestions = 0;
        for (int i = 0; i < events.length(); i++) {
            if ("chat_question_sent".equals(events.getJSONObject(i).optString("event_type"))) {
                recordedChatQuestions++;
            }
        }
        int missingChatQuestions = Math.max(
                0,
                stats(userId).optInt("chat_questions") - recordedChatQuestions
        );
        for (int i = legacyChatHistory.length() - 1; i >= 0 && missingChatQuestions > 0; i--) {
            JSONObject message = legacyChatHistory.optJSONObject(i);
            if (message == null || !message.optBoolean("from_user", false)) continue;
            long timestamp = message.optLong("time", System.currentTimeMillis());
            events.put(new JSONObject()
                    .put("id", "legacy-chat-" + timestamp + "-" + i)
                    .put("user_id", userId)
                    .put("event_type", "chat_question_sent")
                    .put("topic", SUBJECT_NAME)
                    .put("lesson_id", JSONObject.NULL)
                    .put("question_id", JSONObject.NULL)
                    .put("selected_answer", JSONObject.NULL)
                    .put("correct_answer", JSONObject.NULL)
                    .put("is_correct", JSONObject.NULL)
                    .put("difficulty", JSONObject.NULL)
                    .put("response_time_seconds", 0)
                    .put("time_spent_seconds", 0)
                    .put("timestamp", timestamp));
            missingChatQuestions--;
        }
        return new JSONObject()
                .put("schema_version", 2)
                .put("events", events)
                .put("stats", stats(userId))
                .put("adaptive_profile", jsonObject(key(userId, "adaptive_profile"), new JSONObject()))
                .put("completed_lessons", completedLessons(token).length())
                .put("total_lessons", lessonsData().length())
                .put("completed_phases", completedCampaignCount(userId))
                .put("total_phases", campaignData().length());
    }

    public static void startStudySession(String token) throws JSONException {
        int userId = userIdFromToken(token);
        String sessionKey = key(userId, "active_study_session");
        if (jsonObject(sessionKey, null) != null) {
            return;
        }
        long now = System.currentTimeMillis();
        prefs.edit().putString(
                sessionKey,
                new JSONObject().put("id", "session-" + now).put("started_at", now).toString()
        ).apply();
        recordEvent(userId, "study_session_started", JSONObject.NULL, JSONObject.NULL, JSONObject.NULL, JSONObject.NULL, 0);
    }

    public static void completeStudySession(String token) throws JSONException {
        int userId = userIdFromToken(token);
        String sessionKey = key(userId, "active_study_session");
        JSONObject active = jsonObject(sessionKey, null);
        if (active == null) {
            return;
        }
        long elapsedSeconds = (System.currentTimeMillis() - active.optLong("started_at")) / 1000L;
        int duration = (int) Math.min(14_400L, Math.max(5L, elapsedSeconds));
        addStats(userId, 0, 0, 0, 0, duration);
        recordEvent(userId, "study_session_completed", JSONObject.NULL, JSONObject.NULL, JSONObject.NULL, JSONObject.NULL, duration);
        prefs.edit().remove(sessionKey).apply();
    }

    public static JSONObject improvementStats(String token) throws JSONException {
        int userId = userIdFromToken(token);
        JSONObject stats = stats(userId);
        int answered = stats.optInt("answered");
        int correct = stats.optInt("correct");
        int incorrect = Math.max(0, answered - correct);
        int accuracy = answered == 0 ? 0 : Math.round(correct * 100f / answered);
        int completedPhases = completedCampaignCount(userId);
        int completedLessons = completedLessons(token).length();
        int totalLessons = lessonsData().length();
        int trackTotal = dimensionalTrackTotal();
        int trackDone = trackTotal > 0 ? dimensionalTrackCompleted(token) : 0;
        int progress = trackTotal > 0
                ? Math.round(trackDone * 100f / trackTotal)
                : (totalLessons == 0 ? 0 : Math.round(completedLessons * 100f / totalLessons));
        JSONArray events = jsonArray(key(userId, "learning_events"));
        String weakTopic = topicWithMostErrors(events);
        String recommendedDifficulty = recommendedDifficulty(answered, accuracy);
        int averageResponseTime = averageResponseTime(events);
        return new JSONObject()
                .put("accuracy_rate", accuracy)
                .put("study_quality", answered == 0 ? "Comece pela primeira aula" : accuracy >= 80 ? "Muito boa" : accuracy >= 60 ? "Boa evolucao" : "Em desenvolvimento")
                .put("studied_seconds", stats.optInt("study_seconds"))
                .put("completed_lessons", completedLessons)
                .put("total_lessons", totalLessons)
                .put("answered_exercises", answered)
                .put("correct_answers", correct)
                .put("incorrect_answers", incorrect)
                .put("dimensional_progress", progress)
                .put("questions_asked", stats.optInt("chat_questions"))
                .put("completed_phases", completedPhases)
                .put("total_phases", campaignData().length())
                .put("last_lesson", stats.optString("last_lesson_title", "Nenhuma aula aberta ainda"))
                .put("weak_topic", weakTopic)
                .put("recommended_difficulty", recommendedDifficulty)
                .put("next_action", nextAction(answered, accuracy, progress, weakTopic, stats.optInt("chat_questions")))
                .put("average_response_time_seconds", averageResponseTime)
                .put("easy_accuracy_rate", difficultyAccuracy(events, "Facil"))
                .put("medium_accuracy_rate", difficultyAccuracy(events, "Medio"))
                .put("recommendation", recommendation(answered, accuracy, progress, completedPhases, weakTopic));
    }

    public static JSONObject chat(String token, String message) throws JSONException {
        int userId = userIdFromToken(token);
        String cleanMessage = message == null ? "" : message.trim();
        String answer = Tutor.answer(cleanMessage);
        saveChatExchange(token, cleanMessage, answer);
        addStats(userId, 0, 0, 0, 1, 0);
        recordEvent(userId, "chat_question_sent", JSONObject.NULL, JSONObject.NULL, JSONObject.NULL, JSONObject.NULL, 0);

        return new JSONObject()
                .put("session_id", userId)
                .put("user_message", cleanMessage)
                .put("ai_response", answer);
    }

    public static JSONObject saveChatExchange(String token, String message, String answer) throws JSONException {
        int userId = userIdFromToken(token);
        String cleanMessage = message == null ? "" : message.trim();
        String cleanAnswer = answer == null || answer.trim().isEmpty()
                ? Tutor.answer(cleanMessage)
                : answer.trim();
        long now = System.currentTimeMillis();

        JSONArray history = chatHistory(token);
        history.put(new JSONObject().put("from_user", true).put("text", cleanMessage).put("time", now));
        history.put(new JSONObject().put("from_user", false).put("text", cleanAnswer).put("time", now + 1));
        history = trimHistory(history, 60);

        prefs.edit().putString(key(userId, "chat_history"), history.toString()).apply();

        return new JSONObject()
                .put("session_id", userId)
                .put("user_message", cleanMessage)
                .put("ai_response", cleanAnswer);
    }

    public static String localTutorAnswer(String message) {
        return Tutor.answer(message == null ? "" : message.trim());
    }

    public static JSONArray chatHistory(String token) throws JSONException {
        int userId = userIdFromToken(token);
        return jsonArray(key(userId, "chat_history"));
    }

    public static void incrementChatStats(String token) {
        try {
            addStats(userIdFromToken(token), 0, 0, 0, 1, 0);
            emitAppEvent("ChatMessageSent");
        } catch (JSONException ignored) {}
    }

    public static void recordOcrUsed(String token, String topic, int responseTimeSeconds) {
        try {
            int userId = userIdFromToken(token);
            recordEventWithMetrics(
                    userId,
                    "ocr_used",
                    topic == null || topic.trim().isEmpty() ? SUBJECT_NAME : topic.trim(),
                    JSONObject.NULL,
                    JSONObject.NULL,
                    JSONObject.NULL,
                    JSONObject.NULL,
                    Math.max(0, responseTimeSeconds),
                    0,
                    JSONObject.NULL,
                    JSONObject.NULL
            );
            emitAppEvent("OcrAnalyzed");
        } catch (JSONException ignored) {}
    }

    public static void recordVoiceUsed(String token, boolean remoteVoice) {
        try {
            int userId = userIdFromToken(token);
            recordEventWithMetrics(
                    userId,
                    "voice_used",
                    SUBJECT_NAME,
                    JSONObject.NULL,
                    JSONObject.NULL,
                    JSONObject.NULL,
                    remoteVoice ? "remota" : "local",
                    0,
                    0,
                    JSONObject.NULL,
                    JSONObject.NULL
            );
            emitAppEvent("VoiceUsed");
        } catch (JSONException ignored) {}
    }

    public static void saveAdaptiveProfile(
            String token,
            int fuzzyScore,
            String level,
            String nextTopic,
            String learningVelocity
    ) {
        try {
            int userId = userIdFromToken(token);
            JSONObject profile = new JSONObject()
                    .put("fuzzy_score", Math.max(0, Math.min(100, fuzzyScore)))
                    .put("level", level == null ? "Intermediário" : level)
                    .put("next_topic", nextTopic == null ? SUBJECT_NAME : nextTopic)
                    .put("learning_velocity", learningVelocity == null ? "em formação" : learningVelocity)
                    .put("updated_at", System.currentTimeMillis());
            prefs.edit().putString(key(userId, "adaptive_profile"), profile.toString()).apply();
        } catch (JSONException ignored) {}
    }

    public static int computeXp(String token) {
        try {
            int userId = userIdFromToken(token);
            JSONObject s = stats(userId);
            int answered = s.optInt("answered");
            int correct = s.optInt("correct");
            int campaigns = s.optInt("campaign_completed");
            int chats = s.optInt("chat_questions");
            int studySeconds = s.optInt("study_seconds");
            return answered * 10 + correct * 5 + campaigns * 50 + chats * 2 + studySeconds / 60;
        } catch (Exception e) {
            return 0;
        }
    }

    public static int computeLevel(String token) {
        return (int) Math.floor(Math.sqrt(computeXp(token) / 100.0));
    }

    public static void recordLessonOpened(String token, String lessonId) {
        try {
            int userId = userIdFromToken(token);
            recordEvent(userId, "lesson_opened", lessonId, JSONObject.NULL, JSONObject.NULL, JSONObject.NULL, 0);
            emitAppEvent("LessonOpened");
        } catch (JSONException ignored) {}
    }

    public static void recordChatEvent(String token, String eventType, String topic, int responseTimeSeconds) throws JSONException {
        int userId = userIdFromToken(token);
        recordEventWithMetrics(
                userId,
                eventType,
                topic == null || topic.trim().isEmpty() ? SUBJECT_NAME : topic.trim(),
                JSONObject.NULL,
                JSONObject.NULL,
                JSONObject.NULL,
                JSONObject.NULL,
                Math.max(0, responseTimeSeconds),
                0,
                JSONObject.NULL,
                JSONObject.NULL
        );
    }

    public static void recordAuthEvent(String token, String eventType) throws JSONException {
        int userId = userIdFromToken(token);
        recordEvent(userId, eventType, JSONObject.NULL, JSONObject.NULL, JSONObject.NULL, JSONObject.NULL, 0);
    }

    public static JSONObject supportInfo() throws JSONException {
        return new JSONObject()
                .put("title", "Suporte")
                .put("message", "Para recuperar a senha ou relatar um problema, fale com o responsavel pela apresentacao do projeto. Informe seu e-mail e descreva o que aconteceu.")
                .put("email", "suporte@fisicainterativa.local");
    }

    public static JSONArray completedTrackMissions(String token) throws JSONException {
        int userId = userIdFromToken(token);
        return jsonArray(key(userId, "track_completed_dimensional"));
    }

    public static void markTrackMissionCompleted(String token, String missionId) throws JSONException {
        if (missionId == null || missionId.trim().isEmpty()) return;
        int userId = userIdFromToken(token);
        JSONArray completed = jsonArray(key(userId, "track_completed_dimensional"));
        if (!contains(completed, missionId)) {
            completed.put(missionId);
            prefs.edit().putString(key(userId, "track_completed_dimensional"), completed.toString()).apply();
            addStats(userId, 0, 0, 1, 0, 240);
            recordEvent(userId, "track_mission_completed", JSONObject.NULL, missionId, true, "trilha", 240);
            emitAppEvent("TrackMissionCompleted");
        }
    }

    public static void setDimensionalTrackTotal(int total) {
        if (prefs == null) return;
        prefs.edit().putInt("dimensional_track_total", Math.max(0, total)).apply();
    }

    public static int dimensionalTrackTotal() {
        if (prefs == null) return 0;
        return prefs.getInt("dimensional_track_total", 0);
    }

    public static int dimensionalTrackCompleted(String token) throws JSONException {
        return completedTrackMissions(token).length();
    }

    public static double dimensionalTrackRatio(String token) throws JSONException {
        int total = dimensionalTrackTotal();
        if (total <= 0) return 0.0;
        int done = dimensionalTrackCompleted(token);
        return Math.min(1.0, Math.max(0.0, (double) done / total));
    }

    public static JSONObject dailyChallengeInstance(String token, String instanceId) throws JSONException {
        int userId = userIdFromToken(token);
        JSONObject saved = jsonObject(key(userId, "daily_instance_" + instanceId), null);
        if (saved == null) {
            return new JSONObject().put("completed", false);
        }
        return new JSONObject()
                .put("completed", saved.optBoolean("completed", false))
                .put("score", saved.optInt("score", 0))
                .put("total", saved.optInt("total", 0))
                .put("completed_at", saved.optString("completed_at", ""))
                .put("picks", saved.optJSONObject("picks") == null ? new JSONObject() : saved.optJSONObject("picks"));
    }

    public static void recordDailyChallengeInstance(String token, String instanceId, int score, int total, JSONObject picks) throws JSONException {
        int userId = userIdFromToken(token);
        JSONObject saved = new JSONObject()
                .put("completed", true)
                .put("score", score)
                .put("total", total)
                .put("completed_at", today())
                .put("picks", picks == null ? new JSONObject() : picks);
        prefs.edit().putString(key(userId, "daily_instance_" + instanceId), saved.toString()).apply();
        addStats(userId, total, score, 0, 0, 180);
        recordEvent(userId, "daily_instance_completed", JSONObject.NULL, instanceId, score >= total, "misto", 180);
        emitAppEvent("DailyChallengeInstanceSubmitted");
    }

    /** Mantido para back-compat; delega para a sobrecarga com picks. */
    public static void recordDailyChallengeInstance(String token, String instanceId, int score, int total) throws JSONException {
        recordDailyChallengeInstance(token, instanceId, score, total, null);
    }

    public static void recordDailyQuestionUsage(String token, String questionId, long epochDay) throws JSONException {
        if (questionId == null || questionId.trim().isEmpty()) return;
        int userId = userIdFromToken(token);
        JSONObject usage = jsonObject(key(userId, "daily_question_usage"), null);
        if (usage == null) usage = new JSONObject();
        usage.put(questionId, epochDay);
        prefs.edit().putString(key(userId, "daily_question_usage"), usage.toString()).apply();
    }

    public static JSONObject dailyQuestionUsage(String token) throws JSONException {
        int userId = userIdFromToken(token);
        JSONObject usage = jsonObject(key(userId, "daily_question_usage"), null);
        return usage == null ? new JSONObject() : usage;
    }

    public static JSONArray avatarItems() throws JSONException {
        return new JSONArray()
                .put(new JSONObject().put("id", "azul").put("category", "Tema").put("name", "Azul real"))
                .put(new JSONObject().put("id", "claro").put("category", "Tema").put("name", "Modo claro"))
                .put(new JSONObject().put("id", "noturno").put("category", "Tema").put("name", "Modo escuro"));
    }

    private static JSONArray lessonsData() throws JSONException {
        JSONArray list = new JSONArray();
        list.put(lessonData(
                "intro",
                "Introducao a Analise Dimensional",
                "Entenda por que conferir dimensoes evita erros antes dos calculos.",
                "A analise dimensional e uma revisao de sentido fisico. Antes de trocar letras por numeros, ela pergunta: os dois lados da formula falam da mesma coisa?\n\nImagine duas caixas: uma guarda medidas de comprimento e outra guarda medidas de tempo. Voce pode comparar valores dentro da mesma caixa, mas nao pode somar uma medida da caixa de comprimento com uma medida da caixa de tempo.\n\nRegra principal:\nem uma igualdade, os dois lados precisam ter a mesma dimensao.\n\nExemplo visual:\n3 metros representam comprimento.\n2 segundos representam tempo.\n3 m + 2 s nao forma uma grandeza fisica clara.\n\nA analise dimensional funciona como um detector de incoerencia. Se as dimensoes nao combinam, a formula precisa ser revista antes de qualquer conta.",
                1
        ));
        list.put(lessonData(
                "grandezas",
                "Grandezas, medidas e unidades",
                "Separe grandeza, numero e unidade com exemplos do dia a dia.",
                "Grandeza fisica e aquilo que pode ser medido: comprimento, massa, tempo, velocidade, forca, energia e potencia.\n\nMedida e o pacote completo. Em 5 m, o numero e 5 e a unidade e metro.\n\nUnidade e o padrao de comparacao. Metro mede comprimento, segundo mede tempo e quilograma mede massa.\n\nTrocar unidade nao troca a natureza da grandeza:\n100 cm e 1 m continuam sendo comprimento.\n60 s e 1 min continuam sendo tempo.\n\nQuando uma questao parecer confusa, separe em tres partes:\n1. qual grandeza aparece;\n2. qual numero aparece;\n3. qual unidade acompanha esse numero.",
                2
        ));
        list.put(lessonData(
                "fundamentais",
                "Dimensoes fundamentais",
                "Use [M], [L] e [T] para representar massa, comprimento e tempo.",
                "Nesta trilha vamos usar tres dimensoes fundamentais.\n\n[M] representa massa.\n[L] representa comprimento.\n[T] representa tempo.\n\nAs outras grandezas nascem da combinacao dessas tres.\n\nArea:\ncomprimento vezes comprimento\n[A] = [L].[L] = [L]^2\n\nVolume:\ncomprimento vezes comprimento vezes comprimento\n[V] = [L]^3\n\nVelocidade:\ncomprimento dividido por tempo\n[v] = [L][T]^-1\n\nNumeros puros, como 2, 1/2 ou pi, nao mudam a dimensao. Eles mudam o valor da conta, mas nao a natureza fisica.",
                3
        ));
        list.put(lessonData(
                "formulas",
                "Formulas dimensionais",
                "Monte a dimensao de velocidade, aceleracao, forca e energia.",
                "Para montar uma formula dimensional, troque cada grandeza por sua dimensao.\n\nVelocidade:\nv = distancia / tempo\n[v] = [L] / [T] = [L][T]^-1\n\nAceleracao:\na = velocidade / tempo\n[a] = [L][T]^-2\n\nForca:\nF = m.a\n[F] = [M][L][T]^-2\n\nEnergia:\nE = m.v^2\n[E] = [M][L]^2[T]^-2",
                4
        ));
        list.put(lessonData(
                "metodo",
                "Metodo passo a passo",
                "Siga uma sequencia simples para resolver qualquer questao.",
                "Passo 1: destaque as grandezas que aparecem na formula.\n\nPasso 2: escreva a dimensao de cada uma.\n\nPasso 3: substitua as grandezas por [M], [L] e [T].\n\nPasso 4: simplifique as potencias.\n\nPasso 5: compare os dois lados. Se forem diferentes, a expressao esta incoerente.\n\nExemplo guiado:\nqueremos testar x = v.t.\n\n[x] = [L]\n[v] = [L][T]^-1\n[t] = [T]\n\nLado direito:\n[v].[t] = [L][T]^-1.[T] = [L]\n\nOs dois lados viraram [L]. A expressao e coerente.\n\nDica: primeiro resolva com dimensoes. So depois use numeros.",
                5
        ));
        list.put(lessonData(
                "exemplos",
                "Exemplos resolvidos",
                "Veja conferencias completas de formulas comuns.",
                "Exemplo 1: v = d/t\nDistancia e [L]. Tempo e [T].\nO lado direito fica [L]/[T] = [L][T]^-1.\nLogo, velocidade tem dimensao [L][T]^-1.\n\nExemplo 2: F = m.a\nMassa e [M]. Aceleracao e [L][T]^-2.\nO lado direito fica [M].[L][T]^-2 = [M][L][T]^-2.\nEssa e a dimensao de forca.\n\nExemplo 3: x = v + t\nVelocidade e [L][T]^-1. Tempo e [T].\nComo as dimensoes sao diferentes, nao podemos somar esses termos. A expressao esta incoerente.\n\nExemplo 4: E = m.v^2\n[M]([L][T]^-1)^2 = [M][L]^2[T]^-2.\nEssa e a dimensao de energia.\n\nExemplo 5: P = E/t\n[P] = [M][L]^2[T]^-2 / [T] = [M][L]^2[T]^-3.",
                6
        ));
        list.put(lessonData(
                "treino",
                "Treino guiado",
                "Pratique lendo a dimensao antes de escolher uma resposta.",
                "Treino 1: qual a dimensao de area?\nResposta: [L]^2, pois area e comprimento vezes comprimento.\n\nTreino 2: qual a dimensao de aceleracao?\nResposta: [L][T]^-2, pois aceleracao e velocidade dividida por tempo.\n\nTreino 3: uma formula que soma massa com comprimento e valida?\nResposta: nao. Somente grandezas de mesma dimensao podem ser somadas.\n\nTreino 4: por que a analise dimensional nao prova uma formula?\nResposta: ela mostra coerencia de dimensoes, mas nao garante constantes, sinais ou detalhes do modelo fisico.",
                7
        ));
        return list;
    }

    private static JSONObject lessonData(String id, String title, String description, String content, int order) throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("subject_id", SUBJECT_ID)
                .put("subject_name", SUBJECT_NAME)
                .put("title", title)
                .put("description", description)
                .put("content", content)
                .put("exam_tags", SUBJECT_NAME)
                .put("sort_order", order);
    }

    private static JSONObject lessonSummary(JSONObject lesson, boolean completed) throws JSONException {
        return new JSONObject()
                .put("id", lesson.getString("id"))
                .put("title", lesson.getString("title"))
                .put("description", lesson.getString("description"))
                .put("subject_id", SUBJECT_ID)
                .put("subject_name", SUBJECT_NAME)
                .put("exam_tags", lesson.getString("exam_tags"))
                .put("is_completed", completed);
    }

    private static JSONObject lessonDetail(JSONObject lesson, boolean completed) throws JSONException {
        return lessonSummary(lesson, completed)
                .put("content", lesson.getString("content"));
    }

    private static JSONArray campaignData() throws JSONException {
        JSONArray list = new JSONArray();
        list.put(stage(
                "camp-base",
                "Base dimensional",
                "Reconheca grandezas e traduza unidades para dimensoes.",
                "Fase 1",
                "dimension",
                new String[][]{
                        {"camp-base-1", "A medida 12 m representa qual grandeza?", "Comprimento|Tempo|Massa|Energia", "0", "Metro mede comprimento, cuja dimensao e [L]."},
                        {"camp-base-2", "Qual simbolo representa massa?", "[M]|[L]|[T]|[A]", "0", "Massa e representada por [M]."},
                        {"camp-base-3", "Area tem qual dimensao?", "[L]^2|[L]^3|[M][L]|[T]^2", "0", "Area e comprimento vezes comprimento: [L]^2."},
                        {"camp-base-4", "Volume de um cubo depende de tres comprimentos. Qual dimensao aparece?", "[L]^3|[L]^2|[M][L]|[T]^3", "0", "Volume combina largura, altura e profundidade: [L].[L].[L] = [L]^3."}
                }
        ));
        list.put(stage(
                "camp-formulas",
                "Forja de formulas",
                "Monte expressoes dimensionais antes de calcular.",
                "Fase 2",
                "formula",
                new String[][]{
                        {"camp-formulas-1", "Velocidade tem dimensao:", "[L][T]^-1|[L][T]|[M][T]|[T][L]^-2", "0", "Velocidade e distancia dividida pelo tempo."},
                        {"camp-formulas-2", "Aceleracao tem dimensao:", "[L][T]^-2|[M][L]|[T]^2|[M][T]^-1", "0", "Aceleracao e velocidade dividida por tempo."},
                        {"camp-formulas-3", "Forca, em F = m.a, tem dimensao:", "[M][L][T]^-2|[M][T]|[L]^2|[M]^2[L]", "0", "Multiplique massa por aceleracao."},
                        {"camp-formulas-4", "Quantidade de movimento p = m.v tem dimensao:", "[M][L][T]^-1|[M][L]^2[T]^-2|[L][T]^-1|[M][T]^-2", "0", "Massa e [M], velocidade e [L][T]^-1. O produto fica [M][L][T]^-1."}
                }
        ));
        list.put(stage(
                "camp-coerencia",
                "Caca-erros",
                "Ache quais expressoes fazem sentido fisico.",
                "Fase 3",
                "check",
                new String[][]{
                        {"camp-check-1", "A expressao x = v.t e coerente?", "Sim|Nao|So se v for massa|Nunca com tempo", "0", "Velocidade vezes tempo resulta em comprimento."},
                        {"camp-check-2", "A expressao v = d + t e coerente?", "Nao|Sim|Sempre depende do numero|So no SI", "0", "Nao se soma comprimento com tempo."},
                        {"camp-check-3", "Em uma igualdade fisica, os dois lados devem ter:", "A mesma dimensao|O mesmo numero|A mesma letra|A mesma unidade sempre", "0", "A igualdade precisa comparar grandezas da mesma natureza."},
                        {"camp-check-4", "A expressao area = velocidade.tempo e coerente?", "Nao|Sim|So em queda livre|So no grafico", "0", "Velocidade vezes tempo resulta em comprimento [L], nao em area [L]^2."}
                }
        ));
        list.put(stage(
                "camp-final",
                "Desafio final",
                "Use o metodo completo em questoes de maior atencao.",
                "Fase 4",
                "energy",
                new String[][]{
                        {"camp-final-1", "A dimensao de E = m.v^2 e:", "[M][L]^2[T]^-2|[M][L][T]^-2|[L]^2[T]^-1|[M]^2[L]", "0", "Eleve a velocidade ao quadrado e multiplique por massa."},
                        {"camp-final-2", "Se P = F.v, qual a dimensao de potencia?", "[M][L]^2[T]^-3|[M][L][T]^-1|[L][T]^-2|[M][T]^2", "0", "Forca e [M][L][T]^-2; velocidade e [L][T]^-1."},
                        {"camp-final-3", "Um coeficiente sem unidade e chamado de:", "Adimensional|Fundamental|Temporal|Vetorial", "0", "Grandezas sem unidade tem dimensao 1."},
                        {"camp-final-4", "Se uma formula tem coerencia dimensional, isso prova que ela esta certa?", "Nao, so mostra coerencia|Sim, prova tudo|Sim, se tiver metros|Nao, porque dimensao nao existe", "0", "A analise dimensional encontra incoerencias, mas nao garante constantes, sinais ou o modelo completo."}
                }
        ));
        return list;
    }

    private static JSONObject stage(String id, String title, String description, String label, String visual, String[][] exercises) throws JSONException {
        JSONArray exerciseArray = new JSONArray();
        for (String[] exercise : exercises) {
            JSONArray options = new JSONArray();
            String[] values = exercise[2].split("\\|");
            for (String option : values) {
                options.put(option);
            }
            exerciseArray.put(new JSONObject()
                    .put("id", exercise[0])
                    .put("question", exercise[1])
                    .put("options", options)
                    .put("correct_index", Integer.parseInt(exercise[3]))
                    .put("explanation", exercise[4])
                    .put("visual_type", visual));
        }
        return new JSONObject()
                .put("id", id)
                .put("title", title)
                .put("description", description)
                .put("subject_id", SUBJECT_ID)
                .put("subject_name", SUBJECT_NAME)
                .put("stage_label", label)
                .put("visual_type", visual)
                .put("exercises", exerciseArray);
    }

    private static JSONObject question(String id, String question, String[] options, int correct, String explanation, String difficulty) throws JSONException {
        JSONArray optionArray = new JSONArray();
        for (String option : options) {
            optionArray.put(option);
        }
        return new JSONObject()
                .put("id", id)
                .put("question", question)
                .put("options", optionArray)
                .put("correct_index", correct)
                .put("explanation", explanation)
                .put("subject_id", SUBJECT_ID)
                .put("subject_name", SUBJECT_NAME)
                .put("topic", topicForQuestion(id, question))
                .put("difficulty", difficulty);
    }

    private static String topicForQuestion(String id, String question) {
        String text = (id + " " + question).toLowerCase(Locale.ROOT);
        if (text.contains("unidade") || text.contains("metro") || text.contains("segundo")) {
            return "Diferença entre unidade e dimensão";
        }
        if (text.contains("velocidade") || text.contains("forca") || text.contains("energia") || text.contains("potencia")) {
            return "Fórmulas dimensionais";
        }
        return SUBJECT_NAME;
    }

    private static JSONObject authResponse(JSONObject user) throws JSONException {
        return new JSONObject()
                .put("access_token", "local-" + user.getInt("id"))
                .put("token_type", "local")
                .put("user", publicUser(user));
    }

    private static JSONObject publicUser(JSONObject user) throws JSONException {
        return new JSONObject()
                .put("id", user.getInt("id"))
                .put("name", user.optString("name"))
                .put("email", user.optString("email"))
                .put("phone", user.opt("phone"))
                .put("private_account", user.optBoolean("private_account"))
                .put("notifications_enabled", user.optBoolean("notifications_enabled", true));
    }

    private static JSONObject userJson(int id, String name, String email, String password, String phone, boolean privateAccount, boolean notifications) {
        try {
            return new JSONObject()
                    .put("id", id)
                    .put("name", name)
                    .put("email", email)
                    .put("password", password == null ? "" : password)
                    .put("phone", phone == null ? JSONObject.NULL : phone)
                    .put("private_account", privateAccount)
                    .put("notifications_enabled", notifications);
        } catch (JSONException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void addStats(int userId, int answered, int correct, int campaignCompleted, int chatQuestions, int studySeconds) throws JSONException {
        JSONObject stats = stats(userId);
        stats.put("answered", stats.optInt("answered") + answered);
        stats.put("correct", stats.optInt("correct") + correct);
        stats.put("campaign_completed", Math.max(stats.optInt("campaign_completed"), completedCampaignCount(userId)) + campaignCompleted);
        stats.put("chat_questions", stats.optInt("chat_questions") + chatQuestions);
        stats.put("study_seconds", stats.optInt("study_seconds") + studySeconds);
        prefs.edit().putString(key(userId, "stats"), stats.toString()).apply();
    }

    private static void recordEvent(int userId, String eventType, Object lessonId, Object questionId, Object isCorrect, Object difficulty, int timeSpentSeconds) throws JSONException {
        recordEventWithTopic(userId, eventType, SUBJECT_NAME, lessonId, questionId, isCorrect, difficulty, timeSpentSeconds, JSONObject.NULL, JSONObject.NULL);
    }

    private static void recordEventWithTopic(int userId, String eventType, String topic, Object lessonId, Object questionId, Object isCorrect, Object difficulty, int timeSpentSeconds) throws JSONException {
        recordEventWithTopic(userId, eventType, topic, lessonId, questionId, isCorrect, difficulty, timeSpentSeconds, JSONObject.NULL, JSONObject.NULL);
    }

    private static void recordEventWithTopic(
            int userId,
            String eventType,
            String topic,
            Object lessonId,
            Object questionId,
            Object isCorrect,
            Object difficulty,
            int timeSpentSeconds,
            Object selectedAnswer,
            Object correctAnswer
    ) throws JSONException {
        recordEventWithMetrics(
                userId, eventType, topic, lessonId, questionId, isCorrect, difficulty,
                timeSpentSeconds, timeSpentSeconds, selectedAnswer, correctAnswer
        );
    }

    private static void recordEventWithMetrics(
            int userId,
            String eventType,
            String topic,
            Object lessonId,
            Object questionId,
            Object isCorrect,
            Object difficulty,
            int responseTimeSeconds,
            int timeSpentSeconds,
            Object selectedAnswer,
            Object correctAnswer
    ) throws JSONException {
        String safeType = eventType == null ? "" : eventType.trim();
        if (safeType.length() < 2 || safeType.length() > 80) return;
        String safeTopic = topic == null || topic.trim().isEmpty() ? SUBJECT_NAME : topic.trim();
        if (safeTopic.length() > 120) safeTopic = safeTopic.substring(0, 120);
        int safeResponse = Math.max(0, Math.min(14_400, responseTimeSeconds));
        int safeStudy = Math.max(0, Math.min(14_400, timeSpentSeconds));
        JSONArray events = jsonArray(key(userId, "learning_events"));
        if (events.length() > 0) {
            JSONObject last = events.optJSONObject(events.length() - 1);
            long now = System.currentTimeMillis();
            if (last != null
                    && safeType.equals(last.optString("event_type"))
                    && String.valueOf(questionId).equals(String.valueOf(last.opt("question_id")))
                    && now - last.optLong("timestamp") < 1_000L) {
                return;
            }
        }
        events.put(new JSONObject()
                .put("id", "event-" + System.currentTimeMillis() + "-" + events.length())
                .put("user_id", userId)
                .put("event_type", safeType)
                .put("topic", safeTopic)
                .put("lesson_id", lessonId)
                .put("question_id", questionId)
                .put("selected_answer", selectedAnswer)
                .put("correct_answer", correctAnswer)
                .put("is_correct", isCorrect)
                .put("difficulty", difficulty)
                .put("response_time_seconds", safeResponse)
                .put("time_spent_seconds", safeStudy)
                .put("timestamp", System.currentTimeMillis()));
        events = trimHistory(events, 240);
        prefs.edit().putString(key(userId, "learning_events"), events.toString()).apply();
    }

    private static String recommendation(int answered, int accuracy, int progress, int completedPhases, String weakTopic) {
        if (answered == 0 && progress == 0) {
            return "Comece uma aula ou responda um desafio para acompanhar sua evolucao.";
        }
        if (accuracy < 50 && answered > 0) {
            return "Com base nas suas respostas, recomendamos revisar conceitos basicos, unidades e dimensoes antes do proximo desafio.";
        }
        if (accuracy < 80 && answered > 0) {
            return "Com base nas suas respostas, recomendamos praticar exercicios medios e revisar " + weakTopic + " antes do proximo desafio.";
        }
        if (progress < 50) {
            return "Voce esta evoluindo bem. Continue a trilha de Analise Dimensional e faca desafios de dificuldade media.";
        }
        if (completedPhases >= 3 && accuracy >= 80) {
            return "Seu desempenho esta forte. Recomendamos desafios mais dificeis sobre energia, potencia e coerencia dimensional.";
        }
        return "Bom desempenho em formulas dimensionais. Mantenha os desafios diarios para consolidar o conteudo.";
    }

    private static String recommendedDifficulty(int answered, int accuracy) {
        if (answered == 0 || accuracy < 50) {
            return "Fácil";
        }
        if (accuracy < 80) {
            return "Média";
        }
        return "Avançada";
    }

    private static String nextAction(int answered, int accuracy, int progress, String weakTopic, int chatQuestions) {
        if (answered == 0) {
            return "Responda alguns exercícios para receber uma recomendação personalizada.";
        }
        if (chatQuestions > answered) {
            return "Depois das perguntas ao Titio Renato, pratique exercícios sobre " + weakTopic + ".";
        }
        if (accuracy < 50) {
            return "Revise a base e refaça desafios fáceis.";
        }
        if (progress < 70) {
            return "Continue a trilha e pratique exercícios de dificuldade média.";
        }
        return "Avance para desafios mais difíceis e revise fórmulas dimensionais.";
    }

    private static String topicWithMostErrors(JSONArray events) throws JSONException {
        Map<String, Integer> errors = new HashMap<>();
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.getJSONObject(i);
            if ("exercise_answered".equals(event.optString("event_type")) && event.has("is_correct") && !event.optBoolean("is_correct", true)) {
                String topic = event.optString("topic", SUBJECT_NAME);
                errors.put(topic, errors.getOrDefault(topic, 0) + 1);
            }
        }
        String bestTopic = "Fórmulas dimensionais";
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : errors.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestTopic = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return bestTopic;
    }

    private static int averageResponseTime(JSONArray events) throws JSONException {
        int total = 0;
        int count = 0;
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.getJSONObject(i);
            if ("exercise_answered".equals(event.optString("event_type"))) {
                int value = event.optInt("response_time_seconds", 0);
                if (value > 0) {
                    total += value;
                    count++;
                }
            }
        }
        return count == 0 ? 0 : Math.round((float) total / count);
    }

    private static int difficultyAccuracy(JSONArray events, String difficulty) throws JSONException {
        int total = 0;
        int correct = 0;
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.getJSONObject(i);
            if ("exercise_answered".equals(event.optString("event_type")) && difficulty.equals(event.optString("difficulty"))) {
                total++;
                if (event.optBoolean("is_correct", false)) {
                    correct++;
                }
            }
        }
        return total == 0 ? 0 : Math.round(correct * 100f / total);
    }

    private static JSONObject stats(int userId) throws JSONException {
        JSONObject stats = jsonObject(key(userId, "stats"), null);
        return stats == null ? new JSONObject() : stats;
    }

    private static int completedCampaignCount(int userId) throws JSONException {
        int count = 0;
        JSONArray stages = campaignData();
        for (int i = 0; i < stages.length(); i++) {
            JSONObject stage = stages.getJSONObject(i);
            JSONObject saved = jsonObject(key(userId, "campaign_" + stage.optString("id")), null);
            if (saved != null && saved.optInt("total") > 0 && saved.optInt("score") >= saved.optInt("total")) {
                count++;
            }
        }
        return count;
    }

    private static JSONObject quizResult(int score, int total) throws JSONException {
        return new JSONObject()
                .put("score", score)
                .put("total", total)
                .put("accuracy_rate", total == 0 ? 0 : Math.round(score * 100f / total));
    }

    private static JSONArray completedLessons(String token) throws JSONException {
        return jsonArray(key(userIdFromToken(token), "completed_lessons"));
    }

    private static JSONArray users() throws JSONException {
        return new JSONArray(prefs.getString("users", "[]"));
    }

    private static JSONObject findUserByEmail(JSONArray users, String email) throws JSONException {
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (email.equals(user.optString("email"))) {
                return user;
            }
        }
        return null;
    }

    private static JSONObject userFromToken(String token) throws JSONException {
        int id = userIdFromToken(token);
        JSONArray users = users();
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (user.optInt("id") == id) {
                return user;
            }
        }
        return null;
    }

    private static int userIdFromToken(String token) {
        if (token == null || !token.startsWith("local-")) {
            throw new IllegalArgumentException("Sessao invalida.");
        }
        return Integer.parseInt(token.substring("local-".length()));
    }

    private static JSONArray jsonArray(String key) throws JSONException {
        return new JSONArray(prefs.getString(key, "[]"));
    }

    private static JSONObject jsonObject(String key, JSONObject fallback) throws JSONException {
        String raw = prefs.getString(key, null);
        return raw == null ? fallback : new JSONObject(raw);
    }

    private static JSONArray trimHistory(JSONArray history, int max) throws JSONException {
        if (history.length() <= max) {
            return history;
        }
        JSONArray trimmed = new JSONArray();
        for (int i = history.length() - max; i < history.length(); i++) {
            trimmed.put(history.getJSONObject(i));
        }
        return trimmed;
    }

    private static boolean contains(JSONArray array, String value) {
        for (int i = 0; i < array.length(); i++) {
            if (value.equals(array.optString(i))) {
                return true;
            }
        }
        return false;
    }

    private static String key(int userId, String name) {
        return "u_" + userId + "_" + name;
    }

    private static void seedInitialUser() {
        JSONArray users = new JSONArray();
        users.put(userJson(1, "Aluno", "aluno@fisica.com", "123456", null, false, true));
        prefs.edit()
                .putString("users", users.toString())
                .putInt("next_user_id", 2)
                .commit();
    }

    private static void repairUserStorage() throws JSONException {
        JSONArray users = users();
        if (users.length() == 0) {
            seedInitialUser();
            return;
        }

        int nextId = 1;
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            int id = user.optInt("id", nextId);
            if (id <= 0) {
                id = nextId;
                user.put("id", id);
            }

            nextId = Math.max(nextId, id + 1);
            if (!user.has("name") || user.optString("name").trim().isEmpty()) {
                user.put("name", "Aluno");
            }
            if ("Aluno Demo".equals(user.optString("name"))) {
                user.put("name", "Aluno");
            }
            if (!user.has("email")) {
                user.put("email", "");
            }
            if ("aluno@demo.com".equals(user.optString("email"))) {
                user.put("email", "aluno@fisica.com");
            }
            if (!user.has("password")) {
                user.put("password", "");
            }
            if (!user.has("phone")) {
                user.put("phone", JSONObject.NULL);
            }
            if (!user.has("private_account")) {
                user.put("private_account", false);
            }
            if (!user.has("notifications_enabled")) {
                user.put("notifications_enabled", true);
            }
        }

        int storedNextId = prefs.getInt("next_user_id", nextId);
        prefs.edit()
                .putString("users", users.toString())
                .putInt("next_user_id", Math.max(storedNextId, nextId))
                .commit();
    }

    private static void migrateStorage() throws JSONException {
        int currentVersion = prefs.getInt("storage_schema_version", 1);
        if (currentVersion >= 2) {
            return;
        }
        JSONArray allUsers = users();
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < allUsers.length(); i++) {
            int userId = allUsers.getJSONObject(i).optInt("id");
            if (userId <= 0) {
                continue;
            }
            JSONArray events = jsonArray(key(userId, "learning_events"));
            JSONArray normalized = new JSONArray();
            for (int eventIndex = 0; eventIndex < events.length(); eventIndex++) {
                JSONObject event = events.getJSONObject(eventIndex);
                if (!event.has("topic") || event.optString("topic").trim().isEmpty()) {
                    event.put("topic", SUBJECT_NAME);
                }
                if (!event.has("timestamp")) {
                    event.put("timestamp", System.currentTimeMillis());
                }
                normalized.put(event);
            }
            editor.putString(key(userId, "learning_events"), normalized.toString());
        }
        editor.putInt("storage_schema_version", 2).apply();
    }

    private static String cleanEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean looksLikeEmail(String email) {
        int at = email.indexOf('@');
        int dotAfterAt = email.indexOf('.', at + 2);
        return at > 0 && dotAfterAt > at + 1 && dotAfterAt < email.length() - 1;
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
    }

    private static void clearLearningState(int userId) {
        prefs.edit()
                .remove(key(userId, "completed_lessons"))
                .remove(key(userId, "daily_attempt"))
                .remove(key(userId, "stats"))
                .remove(key(userId, "chat_history"))
                .remove(key(userId, "learning_events"))
                .remove(key(userId, "active_study_session"))
                .remove(key(userId, "campaign_camp-base"))
                .remove(key(userId, "campaign_camp-formulas"))
                .remove(key(userId, "campaign_camp-coerencia"))
                .remove(key(userId, "campaign_camp-final"))
                .apply();
    }

    private static void ensure() {
        if (prefs == null) {
            throw new IllegalStateException("LocalBackend.init precisa ser chamado no MainActivity.");
        }
    }

    /**
     * AGP 9 compiles this Java source before exposing built-in Kotlin classes to
     * javac. Resolve the Kotlin event bus at runtime so persisted mutations still
     * notify active ViewModels without a Java-to-Kotlin compile-time dependency.
     */
    private static void emitAppEvent(String eventName) {
        try {
            Class<?> eventClass = Class.forName(
                    "com.example.testes.data.state.AppEvent$" + eventName
            );
            Object event = eventClass.getField("INSTANCE").get(null);
            Class<?> busClass = Class.forName("com.example.testes.data.state.AppStateBus");
            Object bus = busClass.getField("INSTANCE").get(null);
            for (java.lang.reflect.Method method : busClass.getMethods()) {
                if (method.getName().equals("emit") && method.getParameterCount() == 1) {
                    method.invoke(bus, event);
                    return;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Best-effort refresh: the underlying local mutation already succeeded.
        }
    }

    private static final class Tutor {
        private static String answer(String message) {
            String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
            if (lower.contains("velocidade") || lower.contains("veloc")) {
                return "# Dimensão da velocidade\n\n"
                        + "## Explicação\n\n"
                        + "A **velocidade** compara o espaço percorrido com o tempo gasto.\n\n"
                        + "## Pontos principais\n\n"
                        + "1. Distância tem dimensão `[L]`.\n"
                        + "2. Tempo tem dimensão `[T]`.\n"
                        + "3. Dividir por tempo coloca `[T]` no denominador.\n\n"
                        + "$$\n[v] = [L] / [T] = [L][T]^-1\n$$\n\n"
                        + "## Resumo\n\n"
                        + "A velocidade tem dimensão **`[L][T]^-1`**.";
            }
            if (lower.contains("forca") || lower.contains("newton")) {
                return "# Dimensão da força\n\n"
                        + "## Explicação\n\n"
                        + "Partimos da segunda lei de Newton: **força é massa vezes aceleração**.\n\n"
                        + "$$\nF = m \\cdot a\n$$\n\n"
                        + "## Pontos principais\n\n"
                        + "- Massa: `[M]`.\n"
                        + "- Aceleração: `[L][T]^-2`.\n"
                        + "- Multiplicamos as dimensões.\n\n"
                        + "$$\n[F] = [M][L][T]^-2\n$$\n\n"
                        + "## Resumo\n\n"
                        + "Um newton equivale a **`kg·m/s²`**.";
            }
            if (lower.contains("energia") || lower.contains("joule")) {
                return "# Dimensão da energia\n\n"
                        + "## Explicação\n\n"
                        + "Podemos conferir a energia pela relação proporcional `E = mv²`.\n\n"
                        + "## Pontos principais\n\n"
                        + "1. `[m] = [M]`\n"
                        + "2. `[v] = [L][T]^-1`\n"
                        + "3. `[v²] = [L]²[T]^-2`\n\n"
                        + "$$\n[E] = [M][L]^2[T]^-2\n$$\n\n"
                        + "## Resumo\n\n"
                        + "Essa é a dimensão física da energia.";
            }
            if (lower.contains("como") || lower.contains("passo") || lower.contains("resolver")) {
                return "# Como fazer Análise Dimensional\n\n"
                        + "## Passo a passo\n\n"
                        + "1. Marque todas as grandezas da formula.\n"
                        + "2. Troque cada grandeza por [M], [L] e [T].\n"
                        + "3. Simplifique as potencias.\n"
                        + "4. Compare os dois lados.\n\n"
                        + "> Se os lados não tiverem a mesma dimensão, a fórmula não está coerente.\n\n"
                        + "## Exemplo\n\n"
                        + "Mande uma formula e eu confiro com voce, passo por passo.";
            }
            return "# Regra principal\n\n"
                    + "Só podemos somar grandezas da **mesma dimensão**.\n\n"
                    + "## Como testar uma fórmula\n\n"
                    + "1. Troque comprimento por [L].\n"
                    + "2. Troque massa por [M].\n"
                    + "3. Troque tempo por [T].\n"
                    + "4. Simplifique e compare os dois lados.\n\n"
                    + "## Exemplo\n\n"
                    + "$$\n[v] = [L]/[T] = [L][T]^-1\n$$\n\n"
                    + "## Resumo\n\n"
                    + "Diga qual fórmula ou etapa travou e eu confiro com você.";
        }
    }
}
