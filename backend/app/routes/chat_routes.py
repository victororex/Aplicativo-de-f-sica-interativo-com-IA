import sqlite3

from fastapi import APIRouter, Depends, HTTPException, Response
from pydantic import BaseModel

from app.database import get_db
from app.schemas.chat_schema import ChatRequest, ChatResponse, ChatSessionDetailResponse, ChatSessionResponse, MessageResponse
from app.security import get_optional_user
from app.services.ai_service import generate_physics_response
from app.services.speech_service import generate_human_speech

router = APIRouter()


class SpeechRequest(BaseModel):
    text: str


@router.post("/message", response_model=ChatResponse)
def send_message(
    request: ChatRequest,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row | None = Depends(get_optional_user),
):
    user_id = current_user["id"] if current_user else None
    session_id = _get_or_create_session(db, request.session_id, user_id, request.message)

    db.execute(
        """
        INSERT INTO messages (session_id, user_id, sender, content, subject, level)
        VALUES (?, ?, 'user', ?, ?, ?)
        """,
        (session_id, user_id, request.message.strip(), request.subject, request.level),
    )

    ai_response = generate_physics_response(
        user_message=request.message,
        level=request.level,
        subject=request.subject,
    )
    cursor = db.execute(
        """
        INSERT INTO messages (session_id, user_id, sender, content, subject, level)
        VALUES (?, ?, 'ai', ?, ?, ?)
        """,
        (session_id, user_id, ai_response, request.subject, request.level),
    )
    db.execute("UPDATE chat_sessions SET updated_at = CURRENT_TIMESTAMP WHERE id = ?", (session_id,))
    db.commit()

    saved = db.execute("SELECT created_at FROM messages WHERE id = ?", (cursor.lastrowid,)).fetchone()

    return ChatResponse(
        session_id=session_id,
        user_message=request.message,
        ai_response=ai_response,
        created_at=saved["created_at"],
    )


@router.post("/speech")
def synthesize_speech(request: SpeechRequest):
    try:
        audio = generate_human_speech(request.text)
    except RuntimeError as error:
        raise HTTPException(status_code=503, detail=str(error)) from error
    except ValueError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error

    return Response(content=audio, media_type="audio/mpeg")


@router.get("/sessions", response_model=list[ChatSessionResponse])
def list_sessions(
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row | None = Depends(get_optional_user),
):
    if current_user:
        rows = db.execute(
            """
            SELECT id, title, created_at, updated_at
            FROM chat_sessions
            WHERE user_id = ?
            ORDER BY updated_at DESC
            """,
            (current_user["id"],),
        ).fetchall()
    else:
        rows = db.execute(
            """
            SELECT id, title, created_at, updated_at
            FROM chat_sessions
            WHERE user_id IS NULL
            ORDER BY updated_at DESC
            LIMIT 20
            """
        ).fetchall()
    return [ChatSessionResponse(**dict(row)) for row in rows]


@router.get("/sessions/{session_id}", response_model=ChatSessionDetailResponse)
def get_session(
    session_id: int,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row | None = Depends(get_optional_user),
):
    session = _get_session_or_404(db, session_id, current_user["id"] if current_user else None)
    rows = db.execute(
        """
        SELECT id, sender, content, subject, level, created_at
        FROM messages
        WHERE session_id = ?
        ORDER BY id ASC
        """,
        (session_id,),
    ).fetchall()
    return ChatSessionDetailResponse(
        id=session["id"],
        title=session["title"],
        created_at=session["created_at"],
        updated_at=session["updated_at"],
        messages=[MessageResponse(**dict(row)) for row in rows],
    )


@router.delete("/sessions/{session_id}")
def delete_session(
    session_id: int,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row | None = Depends(get_optional_user),
):
    _get_session_or_404(db, session_id, current_user["id"] if current_user else None)
    db.execute("DELETE FROM chat_sessions WHERE id = ?", (session_id,))
    db.commit()
    return {"session_id": session_id, "deleted": True}


def _get_or_create_session(
    db: sqlite3.Connection,
    session_id: int | None,
    user_id: int | None,
    first_message: str,
) -> int:
    if session_id is not None:
        _get_session_or_404(db, session_id, user_id)
        return session_id

    title = first_message.strip().replace("\n", " ")[:80] or "Nova conversa"
    cursor = db.execute(
        "INSERT INTO chat_sessions (user_id, title) VALUES (?, ?)",
        (user_id, title),
    )
    return int(cursor.lastrowid)


def _get_session_or_404(db: sqlite3.Connection, session_id: int, user_id: int | None) -> sqlite3.Row:
    if user_id is None:
        session = db.execute(
            "SELECT * FROM chat_sessions WHERE id = ? AND user_id IS NULL",
            (session_id,),
        ).fetchone()
    else:
        session = db.execute(
            "SELECT * FROM chat_sessions WHERE id = ? AND user_id = ?",
            (session_id, user_id),
        ).fetchone()
    if session is None:
        raise HTTPException(status_code=404, detail="Sessao de chat nao encontrada.")
    return session
