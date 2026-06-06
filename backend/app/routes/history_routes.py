import sqlite3

from fastapi import APIRouter, Depends

from app.database import get_db
from app.schemas.chat_schema import ChatSessionDetailResponse, ChatSessionResponse, MessageResponse
from app.security import get_current_user

router = APIRouter()


@router.get("", response_model=list[ChatSessionResponse])
def list_history(
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row = Depends(get_current_user),
):
    rows = db.execute(
        """
        SELECT id, title, created_at, updated_at
        FROM chat_sessions
        WHERE user_id = ?
        ORDER BY updated_at DESC
        """,
        (current_user["id"],),
    ).fetchall()
    return [ChatSessionResponse(**dict(row)) for row in rows]


@router.get("/{session_id}", response_model=ChatSessionDetailResponse)
def get_history(
    session_id: int,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row = Depends(get_current_user),
):
    session = db.execute(
        """
        SELECT id, title, created_at, updated_at
        FROM chat_sessions
        WHERE id = ? AND user_id = ?
        """,
        (session_id, current_user["id"]),
    ).fetchone()
    if session is None:
        from fastapi import HTTPException

        raise HTTPException(status_code=404, detail="Historico nao encontrado.")

    messages = db.execute(
        """
        SELECT id, sender, content, subject, level, created_at
        FROM messages
        WHERE session_id = ?
        ORDER BY id ASC
        """,
        (session_id,),
    ).fetchall()
    return ChatSessionDetailResponse(
        **dict(session),
        messages=[MessageResponse(**dict(row)) for row in messages],
    )
