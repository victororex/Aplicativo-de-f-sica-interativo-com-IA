package com.example.testes.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
                seedDemoUser();
            } else {
                repairUserStorage();
            }
        } catch (JSONException error) {
            seedDemoUser();
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
                .put("description", "Aprenda a testar formulas usando grandezas, unidades e dimensoes.")
                .put("exam_focus", "Demo focada somente em Analise Dimensional")
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
        JSONArray source = lessonsData();
        JSONArray completed = completedLessons(token);
        for (int i = 0; i < source.length(); i++) {
            JSONObject lesson = source.getJSONObject(i);
            if (lessonId.equals(lesson.optString("id"))) {
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
        }
        prefs.edit().putString(key(userId, "completed_lessons"), completed.toString()).apply();
        return progressSummary(token);
    }

    public static JSONObject progressSummary(String token) throws JSONException {
        int userId = userIdFromToken(token);
        JSONArray completed = completedLessons(token);
        int total = lessonsData().length();
        return new JSONObject()
                .put("user_id", userId)
                .put("completed_lessons", completed)
                .put("current_module", SUBJECT_NAME)
                .put("overall_completion", total == 0 ? 0.0 : (double) completed.length() / total);
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
        return quizResult(score, total);
    }

    public static JSONObject improvementStats(String token) throws JSONException {
        int userId = userIdFromToken(token);
        JSONObject stats = stats(userId);
        int answered = stats.optInt("answered");
        int correct = stats.optInt("correct");
        int accuracy = answered == 0 ? 0 : Math.round(correct * 100f / answered);
        int completedPhases = completedCampaignCount(userId);
        return new JSONObject()
                .put("accuracy_rate", accuracy)
                .put("study_quality", answered == 0 ? "Comece pelo primeiro desafio" : accuracy >= 80 ? "Muito boa" : accuracy >= 60 ? "Boa evolucao" : "Em treino")
                .put("studied_seconds", stats.optInt("study_seconds"))
                .put("questions_asked", stats.optInt("chat_questions"))
                .put("completed_phases", completedPhases)
                .put("total_phases", campaignData().length());
    }

    public static JSONObject chat(String token, String message) throws JSONException {
        int userId = userIdFromToken(token);
        String cleanMessage = message == null ? "" : message.trim();
        String answer = Tutor.answer(cleanMessage);
        saveChatExchange(token, cleanMessage, answer);
        addStats(userId, 0, 0, 0, 1, 45);

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

    public static JSONObject supportInfo() throws JSONException {
        return new JSONObject()
                .put("title", "Suporte")
                .put("message", "Para recuperar a senha ou relatar um problema, fale com o responsavel pela apresentacao do projeto. Informe seu e-mail e descreva o que aconteceu.")
                .put("email", "suporte@fisicainterativa.local");
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
                "Nesta demo vamos usar tres dimensoes fundamentais.\n\n[M] representa massa.\n[L] representa comprimento.\n[T] representa tempo.\n\nAs outras grandezas nascem da combinacao dessas tres.\n\nArea:\ncomprimento vezes comprimento\n[A] = [L].[L] = [L]^2\n\nVolume:\ncomprimento vezes comprimento vezes comprimento\n[V] = [L]^3\n\nVelocidade:\ncomprimento dividido por tempo\n[v] = [L][T]^-1\n\nNumeros puros, como 2, 1/2 ou pi, nao mudam a dimensao. Eles mudam o valor da conta, mas nao a natureza fisica.",
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
                        {"camp-final-4", "Se uma formula passa no teste dimensional, isso prova que ela esta certa?", "Nao, so mostra coerencia|Sim, prova tudo|Sim, se tiver metros|Nao, porque dimensao nao existe", "0", "O teste dimensional encontra erros, mas nao garante constantes, sinais ou o modelo completo."}
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
                .put("difficulty", difficulty);
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

    private static void seedDemoUser() {
        JSONArray users = new JSONArray();
        users.put(userJson(1, "Aluno Demo", "aluno@demo.com", "123456", null, false, true));
        prefs.edit()
                .putString("users", users.toString())
                .putInt("next_user_id", 2)
                .commit();
    }

    private static void repairUserStorage() throws JSONException {
        JSONArray users = users();
        if (users.length() == 0) {
            seedDemoUser();
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
            if (!user.has("email")) {
                user.put("email", "");
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

    private static final class Tutor {
        private static String answer(String message) {
            String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
            if (lower.contains("velocidade") || lower.contains("veloc")) {
                return "Vamos com calma.\n\n"
                        + "Velocidade compara o espaco percorrido com o tempo gasto.\n\n"
                        + "Passos:\n"
                        + "1. Distancia tem dimensao [L].\n"
                        + "2. Tempo tem dimensao [T].\n"
                        + "3. Dividir por tempo significa colocar [T] no denominador.\n\n"
                        + "Formula destacada:\n"
                        + "[v] = [L] / [T] = [L][T]^-1\n\n"
                        + "Entao, velocidade tem dimensao [L][T]^-1.";
            }
            if (lower.contains("forca") || lower.contains("newton")) {
                return "Boa pergunta. Vamos usar F = m.a.\n\n"
                        + "Passos:\n"
                        + "1. Massa tem dimensao [M].\n"
                        + "2. Aceleracao tem dimensao [L][T]^-2.\n"
                        + "3. Multiplicando as duas partes:\n\n"
                        + "[F] = [M][L][T]^-2\n\n"
                        + "Por isso, 1 newton equivale a kg.m/s^2.";
            }
            if (lower.contains("energia") || lower.contains("joule")) {
                return "Vamos conferir energia pela expressao E = m.v^2.\n\n"
                        + "1. [m] = [M]\n"
                        + "2. [v] = [L][T]^-1\n"
                        + "3. [v^2] = [L]^2[T]^-2\n\n"
                        + "Formula dimensional:\n"
                        + "[E] = [M][L]^2[T]^-2\n\n"
                        + "Essa e a dimensao de energia.";
            }
            if (lower.contains("como") || lower.contains("passo") || lower.contains("resolver")) {
                return "O metodo mais seguro e este:\n\n"
                        + "1. Marque todas as grandezas da formula.\n"
                        + "2. Troque cada grandeza por [M], [L] e [T].\n"
                        + "3. Simplifique as potencias.\n"
                        + "4. Compare os dois lados.\n\n"
                        + "Se os dois lados nao tiverem a mesma dimensao, a formula nao esta coerente.\n\n"
                        + "Mande uma formula e eu confiro com voce, passo por passo.";
            }
            return "Vamos pensar como o titio Renato gosta: primeiro a natureza da grandeza, depois os numeros.\n\n"
                    + "Regra principal:\n"
                    + "so podemos somar grandezas da mesma dimensao.\n\n"
                    + "Para testar uma formula:\n"
                    + "1. Troque comprimento por [L].\n"
                    + "2. Troque massa por [M].\n"
                    + "3. Troque tempo por [T].\n"
                    + "4. Simplifique e compare os dois lados.\n\n"
                    + "Exemplo rapido:\n"
                    + "v = d/t fica [v] = [L]/[T] = [L][T]^-1.\n\n"
                    + "Agora me diga a formula ou a parte que travou.";
        }
    }
}
