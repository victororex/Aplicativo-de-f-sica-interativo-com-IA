import sqlite3


def get_improvement_stats(db: sqlite3.Connection, user_id: int | None = None) -> dict:
    filters = []
    params: list[object] = []
    if user_id is None:
        filters.append("user_id IS NULL")
    else:
        filters.append("user_id = ?")
        params.append(user_id)

    where_clause = " AND ".join(filters)
    question_row = db.execute(
        f"""
        SELECT
            COUNT(*) AS questions_asked,
            MIN(created_at) AS first_interaction_at,
            MAX(created_at) AS last_interaction_at
        FROM messages
        WHERE sender = 'user' AND {where_clause}
        """,
        params,
    ).fetchone()

    progress = None
    if user_id is not None:
        progress = db.execute("SELECT * FROM study_progress WHERE user_id = ?", (user_id,)).fetchone()

    answered_exercises = int(progress["answered_exercises"]) if progress else 0
    correct_answers = int(progress["correct_answers"]) if progress else 0
    completed_phases = int(progress["completed_phases"]) if progress else 0
    total_phases = int(progress["total_phases"]) if progress else 20

    if answered_exercises > 0:
        accuracy_rate = round((correct_answers / answered_exercises) * 100)
    else:
        accuracy_rate = 0

    questions_asked = int(question_row["questions_asked"] or 0)
    studied_seconds = questions_asked * 180

    study_quality = "Sem dados"
    if questions_asked >= 10:
        study_quality = "Alta"
    elif questions_asked >= 3:
        study_quality = "Em andamento"

    return {
        "accuracy_rate": accuracy_rate,
        "study_quality": study_quality,
        "studied_seconds": studied_seconds,
        "questions_asked": questions_asked,
        "completed_phases": completed_phases,
        "total_phases": total_phases,
    }
