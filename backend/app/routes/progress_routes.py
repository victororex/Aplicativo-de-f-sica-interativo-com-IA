import sqlite3

from fastapi import APIRouter, Depends, HTTPException

from app.database import get_db
from app.routes.content_routes import _subjects_with_progress
from app.schemas.content_schema import ProgressSummaryResponse
from app.security import get_current_user

router = APIRouter()


@router.get("/summary", response_model=ProgressSummaryResponse)
def progress_summary(
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row = Depends(get_current_user),
):
    subjects = _subjects_with_progress(db, current_user["id"])
    completed = db.execute(
        """
        SELECT lesson_id
        FROM lesson_progress
        WHERE user_id = ?
        ORDER BY completed_at DESC
        """,
        (current_user["id"],),
    ).fetchall()
    total_lessons = sum(subject.total_lessons for subject in subjects)
    completed_count = len(completed)
    current = next((subject.name for subject in subjects if not subject.is_completed), "Todas as materias")
    return ProgressSummaryResponse(
        user_id=current_user["id"],
        overall_completion=completed_count / total_lessons if total_lessons else 0.0,
        completed_lessons=[row["lesson_id"] for row in completed],
        current_module=current,
        subjects=subjects,
    )


@router.post("/lessons/{lesson_id}/complete", response_model=ProgressSummaryResponse)
def complete_lesson(
    lesson_id: str,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row = Depends(get_current_user),
):
    lesson = db.execute("SELECT id FROM lessons WHERE id = ?", (lesson_id,)).fetchone()
    if lesson is None:
        raise HTTPException(status_code=404, detail="Aula nao encontrada.")

    db.execute(
        """
        INSERT INTO lesson_progress (user_id, lesson_id)
        VALUES (?, ?)
        ON CONFLICT(user_id, lesson_id) DO NOTHING
        """,
        (current_user["id"], lesson_id),
    )
    db.execute(
        """
        UPDATE study_progress
        SET completed_phases = (
            SELECT COUNT(*) FROM lesson_progress WHERE user_id = ?
        ),
        total_phases = (SELECT COUNT(*) FROM lessons),
        updated_at = CURRENT_TIMESTAMP
        WHERE user_id = ?
        """,
        (current_user["id"], current_user["id"]),
    )
    db.execute(
        """
        INSERT INTO analytics_events (
            user_id, event_type, topic, time_spent_seconds, metadata_json
        )
        SELECT ?, 'lesson_completed', s.name, 210, json_object('lesson_id', l.id)
        FROM lessons l JOIN subjects s ON s.id = l.subject_id
        WHERE l.id = ?
        """,
        (current_user["id"], lesson_id),
    )
    db.commit()
    return progress_summary(db=db, current_user=current_user)
