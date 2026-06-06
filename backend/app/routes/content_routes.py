import sqlite3

from fastapi import APIRouter, Depends, HTTPException

from app.database import get_db
from app.schemas.content_schema import LessonDetailResponse, LessonSummaryResponse, SubjectResponse
from app.security import get_optional_user

router = APIRouter()


@router.get("/subjects", response_model=list[SubjectResponse])
def list_subjects(
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row | None = Depends(get_optional_user),
):
    return _subjects_with_progress(db, current_user["id"] if current_user else None)


@router.get("/subjects/{subject_id}/lessons", response_model=list[LessonSummaryResponse])
def list_lessons(
    subject_id: str,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row | None = Depends(get_optional_user),
):
    subject = db.execute("SELECT id FROM subjects WHERE id = ?", (subject_id,)).fetchone()
    if subject is None:
        raise HTTPException(status_code=404, detail="Materia nao encontrada.")

    return _lessons_for_subject(db, subject_id, current_user["id"] if current_user else None)


@router.get("/lessons/{lesson_id}", response_model=LessonDetailResponse)
def get_lesson(
    lesson_id: str,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row | None = Depends(get_optional_user),
):
    user_id = current_user["id"] if current_user else None
    params: list[object] = [lesson_id]
    completed_join = "0 AS is_completed"
    if user_id is not None:
        completed_join = """
        CASE WHEN lp.lesson_id IS NULL THEN 0 ELSE 1 END AS is_completed
        """
        params.append(user_id)

    row = db.execute(
        f"""
        SELECT
            l.id,
            l.subject_id,
            s.name AS subject_name,
            l.title,
            l.description,
            l.content,
            l.exam_tags,
            {completed_join}
        FROM lessons l
        JOIN subjects s ON s.id = l.subject_id
        {"LEFT JOIN lesson_progress lp ON lp.lesson_id = l.id AND lp.user_id = ?" if user_id is not None else ""}
        WHERE l.id = ?
        """,
        params[::-1] if user_id is not None else params,
    ).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="Aula nao encontrada.")
    data = dict(row)
    data["is_completed"] = bool(row["is_completed"])
    return LessonDetailResponse(**data)


def _subjects_with_progress(db: sqlite3.Connection, user_id: int | None) -> list[SubjectResponse]:
    params: list[object] = []
    progress_join = ""
    completed_count = "0"
    if user_id is not None:
        progress_join = "LEFT JOIN lesson_progress lp ON lp.lesson_id = l.id AND lp.user_id = ?"
        params.append(user_id)
        completed_count = "COUNT(lp.lesson_id)"

    rows = db.execute(
        f"""
        SELECT
            s.id,
            s.name,
            s.description,
            s.exam_focus,
            COUNT(l.id) AS total_lessons,
            {completed_count} AS completed_lessons
        FROM subjects s
        LEFT JOIN lessons l ON l.subject_id = s.id
        {progress_join}
        GROUP BY s.id, s.name, s.description, s.exam_focus, s.sort_order
        ORDER BY s.sort_order ASC
        """,
        params,
    ).fetchall()
    subjects = []
    for row in rows:
        total = int(row["total_lessons"] or 0)
        completed = int(row["completed_lessons"] or 0)
        progress = completed / total if total else 0.0
        subjects.append(
            SubjectResponse(
                id=row["id"],
                name=row["name"],
                description=row["description"],
                exam_focus=row["exam_focus"],
                total_lessons=total,
                completed_lessons=completed,
                progress=progress,
                is_completed=total > 0 and completed == total,
            )
        )
    return subjects


def _lessons_for_subject(db: sqlite3.Connection, subject_id: str, user_id: int | None) -> list[LessonSummaryResponse]:
    params: list[object] = []
    progress_join = ""
    completed_expr = "0 AS is_completed"
    if user_id is not None:
        progress_join = "LEFT JOIN lesson_progress lp ON lp.lesson_id = l.id AND lp.user_id = ?"
        completed_expr = "CASE WHEN lp.lesson_id IS NULL THEN 0 ELSE 1 END AS is_completed"
        params.append(user_id)
    params.append(subject_id)
    rows = db.execute(
        f"""
        SELECT
            l.id,
            l.subject_id,
            s.name AS subject_name,
            l.title,
            l.description,
            l.exam_tags,
            {completed_expr}
        FROM lessons l
        JOIN subjects s ON s.id = l.subject_id
        {progress_join}
        WHERE l.subject_id = ?
        ORDER BY l.sort_order ASC
        """,
        params,
    ).fetchall()
    lessons = []
    for row in rows:
        data = dict(row)
        data["is_completed"] = bool(row["is_completed"])
        lessons.append(LessonSummaryResponse(**data))
    return lessons
