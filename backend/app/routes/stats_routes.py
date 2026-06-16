import json
import sqlite3

from fastapi import APIRouter, Depends, HTTPException

from app.database import get_db
from app.schemas.stats_schema import (
    AnalyticsEventRequest,
    ImprovementStatsResponse,
    LearningDashboardResponse,
    StudySessionRequest,
    StudySessionResponse,
)
from app.security import get_optional_user
from app.services.stats_service import get_improvement_stats, get_learning_dashboard

router = APIRouter()


@router.get("/improvement", response_model=ImprovementStatsResponse)
def improvement_stats(
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row | None = Depends(get_optional_user),
):
    return get_improvement_stats(db=db, user_id=current_user["id"] if current_user else None)


@router.get("/dashboard", response_model=LearningDashboardResponse)
def learning_dashboard(
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row | None = Depends(get_optional_user),
):
    return get_learning_dashboard(db=db, user_id=current_user["id"] if current_user else None)


@router.post("/events", status_code=201)
def record_event(
    request: AnalyticsEventRequest,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row | None = Depends(get_optional_user),
):
    cursor = db.execute(
        """
        INSERT INTO analytics_events (
            user_id, event_type, topic, is_correct, difficulty,
            response_time_seconds, time_spent_seconds, metadata_json
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (
            current_user["id"] if current_user else None,
            request.event_type,
            request.topic,
            request.is_correct,
            request.difficulty,
            request.response_time_seconds,
            request.time_spent_seconds,
            json.dumps(request.metadata, ensure_ascii=False),
        ),
    )
    db.commit()
    return {"id": cursor.lastrowid, "recorded": True}


@router.post("/sessions", response_model=StudySessionResponse, status_code=201)
def start_session(
    request: StudySessionRequest,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row | None = Depends(get_optional_user),
):
    user_id = current_user["id"] if current_user else None
    cursor = db.execute(
        "INSERT INTO study_sessions (user_id, topic) VALUES (?, ?)",
        (user_id, request.topic),
    )
    db.commit()
    return _session_or_404(db, int(cursor.lastrowid), user_id)


@router.post("/sessions/{session_id}/complete", response_model=StudySessionResponse)
def complete_session(
    session_id: int,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row | None = Depends(get_optional_user),
):
    user_id = current_user["id"] if current_user else None
    _session_or_404(db, session_id, user_id)
    db.execute(
        """
        UPDATE study_sessions
        SET completed_at = CURRENT_TIMESTAMP,
            duration_seconds = MIN(14400, MAX(1, CAST((julianday(CURRENT_TIMESTAMP) - julianday(started_at)) * 86400 AS INTEGER))),
            is_completed = 1
        WHERE id = ?
        """,
        (session_id,),
    )
    db.commit()
    return _session_or_404(db, session_id, user_id)


def _session_or_404(db: sqlite3.Connection, session_id: int, user_id: int | None) -> dict:
    clause = "user_id IS NULL" if user_id is None else "user_id = ?"
    params = (session_id,) if user_id is None else (session_id, user_id)
    row = db.execute(
        f"""
        SELECT id, topic, started_at, completed_at, duration_seconds, is_completed
        FROM study_sessions WHERE id = ? AND {clause}
        """,
        params,
    ).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="Sessao de estudo nao encontrada.")
    data = dict(row)
    data["is_completed"] = bool(data["is_completed"])
    return data
