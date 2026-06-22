from __future__ import annotations

from collections import defaultdict
from datetime import datetime, timedelta
import sqlite3

from app.services.fuzzy_learning_service import (
    FuzzyInputs,
    infer_fuzzy_level,
    response_speed_from_seconds,
)


def get_improvement_stats(db: sqlite3.Connection, user_id: int | None = None) -> dict:
    dashboard = get_learning_dashboard(db, user_id)
    return {
        "accuracy_rate": dashboard["accuracy_rate"],
        "study_quality": _study_quality(dashboard["questions_answered"], dashboard["accuracy_rate"]),
        "studied_seconds": dashboard["total_study_seconds"],
        "questions_asked": dashboard["questions_asked"],
        "completed_phases": dashboard["completed_phases"],
        "total_phases": dashboard["total_phases"],
    }


def get_learning_dashboard(
    db: sqlite3.Connection,
    user_id: int | None,
    now: datetime | None = None,
) -> dict:
    now = now or datetime.now()
    clause, params = _user_filter(user_id)
    events = db.execute(
        f"""
        SELECT id, event_type, topic, is_correct, difficulty,
               response_time_seconds, time_spent_seconds, created_at
        FROM analytics_events
        WHERE {clause}
        ORDER BY created_at DESC, id DESC
        LIMIT 2000
        """,
        params,
    ).fetchall()
    attempts = [row for row in events if row["event_type"] == "exercise_answered" and row["is_correct"] is not None]
    progress = (
        db.execute("SELECT * FROM study_progress WHERE user_id = ?", (user_id,)).fetchone()
        if user_id is not None
        else None
    )
    legacy_answered = int(progress["answered_exercises"]) if progress else 0
    legacy_correct = int(progress["correct_answers"]) if progress else 0
    answered = len(attempts) or legacy_answered
    correct = sum(bool(row["is_correct"]) for row in attempts) if attempts else legacy_correct
    accuracy = _percentage(correct, answered)

    topic_performance = _topic_performance(attempts)
    difficult_topics = [topic for topic in topic_performance if topic["needs_review"]][:3]
    recent_accuracy = _accuracy(attempts[:5])
    previous_accuracy = _accuracy(attempts[5:10])
    trend = _trend(len(attempts), recent_accuracy, previous_accuracy)
    total_lessons = int(db.execute("SELECT COUNT(*) AS total FROM lessons").fetchone()["total"])
    completed_lessons = 0
    if user_id is not None:
        completed_lessons = int(
            db.execute(
                "SELECT COUNT(*) AS total FROM lesson_progress WHERE user_id = ?",
                (user_id,),
            ).fetchone()["total"]
        )
    total_phases = int(progress["total_phases"]) if progress else total_lessons
    completed_phases = int(progress["completed_phases"]) if progress else completed_lessons
    response_times = [int(row["response_time_seconds"]) for row in attempts if row["response_time_seconds"] > 0]
    average_response = round(sum(response_times) / len(response_times)) if response_times else 0
    active_days = len({_parse_date(row["created_at"]).date() for row in events})
    fuzzy = infer_fuzzy_level(
        FuzzyInputs(
            accuracy=float(recent_accuracy if attempts else 45),
            response_speed=response_speed_from_seconds(average_response),
            study_frequency=min(100.0, active_days * 100.0 / 12.0),
            mission_progress=float(_percentage(completed_phases, total_phases)),
            lesson_progress=float(_percentage(completed_lessons, total_lessons)),
        )
    )
    difficulty = fuzzy.level
    next_topic = (
        difficult_topics[0]["topic"]
        if difficult_topics
        else topic_performance[0]["topic"]
        if topic_performance
        else "Fisica"
    )
    learning_velocity = (
        "em formação" if len(attempts) < 4
        else "acelerando" if recent_accuracy >= previous_accuracy + 10
        else "desacelerando" if recent_accuracy + 10 <= previous_accuracy
        else "constante"
    )
    attempted_topics = {(row["topic"] or "Fisica").strip().lower() for row in attempts}
    ignored_topics = list(dict.fromkeys(
        (row["topic"] or "Fisica").strip()
        for row in events
        if (row["topic"] or "Fisica").strip().lower() not in attempted_topics
    ))[:3]
    profile = {
        "explanation_level": _explanation_level(len(attempts), recent_accuracy),
        "exercise_difficulty": difficulty,
        "trend": trend,
        "next_topic": next_topic,
        "review_topics": [topic["topic"] for topic in difficult_topics],
        "suggested_questions": [
            f"Explique {next_topic} no nivel {difficulty}.",
            f"Mostre um exemplo resolvido de {next_topic}.",
            f"Quais erros devo evitar em {next_topic}?",
            f"Crie uma questao {difficulty} sobre {next_topic}.",
        ],
        "fuzzy_score": fuzzy.score,
        "learning_velocity": learning_velocity,
        "ignored_topics": ignored_topics,
    }

    session_row = db.execute(
        f"""
        SELECT COUNT(*) AS completed, COALESCE(SUM(duration_seconds), 0) AS seconds
        FROM study_sessions WHERE {clause} AND is_completed = 1
        """,
        params,
    ).fetchone()
    chat_row = db.execute(
        f"SELECT COUNT(*) AS total FROM messages WHERE sender = 'user' AND {clause}",
        params,
    ).fetchone()
    recommendations = _recommendations(profile, difficult_topics, answered, accuracy)
    session_events_seconds = sum(
        max(0, int(row["time_spent_seconds"]))
        for row in events
        if row["event_type"] == "study_session_completed"
    )
    if user_id is not None:
        try:
            db.execute(
                """
                INSERT INTO adaptive_profiles (
                    user_id, fuzzy_score, level, next_topic, learning_velocity, updated_at
                ) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(user_id) DO UPDATE SET
                    fuzzy_score = excluded.fuzzy_score,
                    level = excluded.level,
                    next_topic = excluded.next_topic,
                    learning_velocity = excluded.learning_velocity,
                    updated_at = CURRENT_TIMESTAMP
                """,
                (user_id, fuzzy.score, fuzzy.level, next_topic, learning_velocity),
            )
            db.commit()
        except sqlite3.OperationalError:
            pass
    return {
        "total_study_seconds": max(int(session_row["seconds"]), session_events_seconds),
        "questions_answered": answered,
        "questions_asked": int(chat_row["total"]),
        "completed_sessions": int(session_row["completed"]),
        "completed_lessons": completed_lessons,
        "total_lessons": total_lessons,
        "completed_phases": completed_phases,
        "total_phases": total_phases,
        "topics_studied": [topic["topic"] for topic in topic_performance],
        "difficult_topics": difficult_topics,
        "topic_performance": topic_performance,
        "daily_evolution": _series(events, attempts, now, "day", 7),
        "weekly_evolution": _series(events, attempts, now, "week", 6),
        "monthly_evolution": _series(events, attempts, now, "month", 6),
        "accuracy_rate": accuracy,
        "average_response_time_seconds": average_response,
        "performance_history": [
            _history_item(row)
            for row in events
            if row["event_type"] in HISTORY_EVENTS
        ][:20],
        "recommendations": recommendations,
        "adaptive_profile": profile,
        "ai_interactions": sum(row["event_type"] == "chat_question_sent" for row in events),
        "ocr_uses": sum(row["event_type"] == "ocr_used" for row in events),
        "voice_uses": sum(row["event_type"] == "voice_used" for row in events),
        "missions_completed": sum(
            row["event_type"] in {
                "campaign_stage_completed", "track_mission_completed",
                "daily_instance_completed", "daily_challenge_completed",
            }
            for row in events
        ),
        "active_study_days": active_days,
    }


def _topic_performance(attempts: list[sqlite3.Row]) -> list[dict]:
    grouped: dict[str, list[sqlite3.Row]] = defaultdict(list)
    for row in attempts:
        grouped[(row["topic"] or "Fisica").strip() or "Fisica"].append(row)
    result = []
    for topic, rows in grouped.items():
        correct = sum(bool(row["is_correct"]) for row in rows)
        accuracy = _percentage(correct, len(rows))
        times = [int(row["response_time_seconds"]) for row in rows if row["response_time_seconds"] > 0]
        average_time = round(sum(times) / len(times)) if times else 0
        speed = 100 if 0 < average_time <= 25 else 75 if average_time <= 50 else 50 if average_time <= 90 else 30
        confidence = round(min(len(rows), 8) / 8 * 100)
        mastery = round(accuracy * 0.7 + speed * 0.15 + confidence * 0.15)
        result.append(
            {
                "topic": topic,
                "attempts": len(rows),
                "correct_answers": correct,
                "accuracy_rate": accuracy,
                "average_response_time_seconds": average_time,
                "mastery_score": mastery,
                "needs_review": len(rows) >= 2 and mastery < 65,
            }
        )
    return sorted(result, key=lambda item: (item["mastery_score"], -item["attempts"]))


def _series(
    events: list[sqlite3.Row],
    attempts: list[sqlite3.Row],
    now: datetime,
    period: str,
    count: int,
) -> list[dict]:
    output = []
    for offset in reversed(range(count)):
        start, end, label = _period_bounds(now, period, offset)
        period_attempts = [row for row in attempts if start <= _parse_date(row["created_at"]) < end]
        period_events = [row for row in events if start <= _parse_date(row["created_at"]) < end]
        output.append(
            {
                "label": label,
                "accuracy_rate": _accuracy(period_attempts),
                "study_seconds": sum(max(0, int(row["time_spent_seconds"])) for row in period_events),
                "questions": len(period_attempts),
                "activities": sum(row["event_type"] in ACTIVITY_EVENTS for row in period_events),
            }
        )
    return output


def _period_bounds(now: datetime, period: str, offset: int) -> tuple[datetime, datetime, str]:
    if period == "day":
        start = (now - timedelta(days=offset)).replace(hour=0, minute=0, second=0, microsecond=0)
        return start, start + timedelta(days=1), start.strftime("%d/%m")
    if period == "week":
        current = now - timedelta(days=now.weekday(), weeks=offset)
        start = current.replace(hour=0, minute=0, second=0, microsecond=0)
        return start, start + timedelta(weeks=1), f"S{start.isocalendar().week}"
    month_index = now.year * 12 + now.month - 1 - offset
    year, month_zero = divmod(month_index, 12)
    start = datetime(year, month_zero + 1, 1)
    next_index = month_index + 1
    next_year, next_month_zero = divmod(next_index, 12)
    return start, datetime(next_year, next_month_zero + 1, 1), start.strftime("%m/%y")


def _recommendations(profile: dict, weak: list[dict], answered: int, accuracy: int) -> list[dict]:
    if answered == 0:
        return [{
            "title": "Crie sua linha de base",
            "reason": "Ainda nao ha respostas suficientes para medir seu dominio.",
            "topic": "Fisica",
            "action": "Conclua o desafio diario.",
            "priority": 1,
        }]
    items = [
        {
            "title": f"Revisar {topic['topic']}",
            "reason": f"Dominio estimado em {topic['mastery_score']}% com {topic['accuracy_rate']}% de acertos.",
            "topic": topic["topic"],
            "action": f"Revise a explicacao e responda 3 questoes {profile['exercise_difficulty'].lower()}.",
            "priority": index + 1,
        }
        for index, topic in enumerate(weak)
    ]
    if not items:
        items.append({
            "title": "Avancar com consistencia",
            "reason": f"Sua taxa atual e de {accuracy}% e nao ha topicos criticos.",
            "topic": profile["next_topic"],
            "action": f"Continue no nivel {profile['exercise_difficulty'].lower()}.",
            "priority": 1,
        })
    return items[:4]


def _history_item(row: sqlite3.Row) -> dict:
    event_type = row["event_type"]
    activity, result, difficulty = {
        "exercise_answered": (
            "Exercício",
            "Correta" if row["is_correct"] else "Revisar",
            row["difficulty"] or "Não informada",
        ),
        "chat_question_sent": ("Chat com Renato", "Pergunta enviada", "IA"),
        "lesson_opened": ("Aula", "Aula acessada", "Estudo"),
        "lesson_completed": ("Aula", "Aula concluída", "Estudo"),
        "ocr_used": ("Análise de imagem", "OCR concluído", "IA"),
        "voice_used": ("Voz personalizada", "Áudio reproduzido", "IA"),
        "campaign_stage_completed": ("Missão", "Missão concluída", row["difficulty"] or "Trilha"),
        "track_mission_completed": ("Missão", "Missão concluída", row["difficulty"] or "Trilha"),
        "daily_instance_completed": ("Desafio diário", "Desafio concluído", row["difficulty"] or "Misto"),
        "daily_challenge_completed": ("Desafio diário", "Desafio concluído", row["difficulty"] or "Misto"),
    }.get(event_type, ("Atividade", "Concluída", row["difficulty"] or "Geral"))
    return {
        "id": str(row["id"]),
        "topic": row["topic"] or "Fisica",
        "activity": activity,
        "result": result,
        "difficulty": difficulty,
        "response_time_seconds": int(row["response_time_seconds"]),
        "timestamp": _parse_date(row["created_at"]),
    }


def _user_filter(user_id: int | None) -> tuple[str, tuple]:
    return ("user_id IS NULL", ()) if user_id is None else ("user_id = ?", (user_id,))


def _parse_date(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00")).replace(tzinfo=None)


def _percentage(value: int, total: int) -> int:
    return round(value / total * 100) if total else 0


def _accuracy(rows: list[sqlite3.Row]) -> int:
    return _percentage(sum(bool(row["is_correct"]) for row in rows), len(rows))


def _trend(attempts: int, recent: int, previous: int) -> str:
    if attempts < 4:
        return "dados_em_formacao"
    if recent >= previous + 10:
        return "evoluindo"
    if recent + 10 <= previous:
        return "atencao"
    return "estavel"


ACTIVITY_EVENTS = {
    "lesson_opened", "lesson_completed", "exercise_answered", "chat_question_sent",
    "ocr_used", "voice_used", "track_mission_completed", "campaign_stage_completed",
    "daily_instance_completed", "daily_challenge_completed",
}
HISTORY_EVENTS = ACTIVITY_EVENTS


def _difficulty(attempts: int, recent: int, topics: list[dict]) -> str:
    if attempts < 3 or recent < 55 or any(item["attempts"] >= 2 and item["accuracy_rate"] < 45 for item in topics):
        return "Facil"
    return "Avancada" if recent >= 85 and attempts >= 6 else "Media"


def _explanation_level(attempts: int, recent: int) -> str:
    if attempts < 3 or recent < 55:
        return "fundamental, com analogias e passos curtos"
    if recent < 80:
        return "intermediario, com exemplos guiados"
    return "avancado, direto e com desafios de vestibular"


def _study_quality(answered: int, accuracy: int) -> str:
    if answered == 0:
        return "Sem dados"
    if accuracy >= 80:
        return "Alta"
    return "Em andamento" if accuracy >= 55 else "Revisao recomendada"
