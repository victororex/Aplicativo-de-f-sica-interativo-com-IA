from pathlib import Path
from uuid import uuid4
import sqlite3

from fastapi import APIRouter, Depends, File, HTTPException, UploadFile, status

from app.config import settings
from app.database import get_db
from app.schemas.file_schema import FileResponse, FileUploadResponse
from app.security import get_current_user

router = APIRouter()


def _upload_root() -> Path:
    path = settings.upload_dir
    if not path.is_absolute():
        path = Path(__file__).resolve().parents[2] / path
    path.mkdir(parents=True, exist_ok=True)
    return path


@router.post("/upload", response_model=FileUploadResponse, status_code=status.HTTP_201_CREATED)
async def upload_file(
    file: UploadFile = File(...),
    session_id: int | None = None,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row = Depends(get_current_user),
):
    contents = await file.read()
    if not contents:
        raise HTTPException(status_code=422, detail="Arquivo vazio.")
    if len(contents) > settings.max_upload_bytes:
        raise HTTPException(status_code=413, detail=f"Arquivo excede {settings.max_upload_mb} MB.")

    original_name = Path(file.filename or "arquivo").name
    extension = Path(original_name).suffix.lower()
    allowed_extensions = {".png", ".jpg", ".jpeg", ".webp", ".pdf", ".docx", ".txt"}
    if extension not in allowed_extensions:
        raise HTTPException(status_code=415, detail="Tipo de arquivo nao suportado.")

    stored_name = f"{uuid4().hex}{extension}"
    stored_path = _upload_root() / stored_name
    stored_path.write_bytes(contents)

    cursor = db.execute(
        """
        INSERT INTO uploaded_files (user_id, session_id, file_name, file_type, file_path, file_size)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        (current_user["id"], session_id, original_name, file.content_type, str(stored_path), len(contents)),
    )
    db.commit()
    saved = db.execute("SELECT * FROM uploaded_files WHERE id = ?", (cursor.lastrowid,)).fetchone()
    return FileUploadResponse(
        id=saved["id"],
        file_name=saved["file_name"],
        file_type=saved["file_type"],
        file_size=saved["file_size"],
        created_at=saved["created_at"],
        message="Arquivo enviado e registrado com sucesso.",
    )


@router.get("/{file_id}", response_model=FileResponse)
def get_file(
    file_id: int,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row = Depends(get_current_user),
):
    row = db.execute(
        "SELECT * FROM uploaded_files WHERE id = ? AND user_id = ?",
        (file_id, current_user["id"]),
    ).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="Arquivo nao encontrado.")
    return FileResponse(
        id=row["id"],
        file_name=row["file_name"],
        file_type=row["file_type"],
        file_size=row["file_size"],
        created_at=row["created_at"],
    )


@router.delete("/{file_id}")
def delete_file(
    file_id: int,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row = Depends(get_current_user),
):
    row = db.execute(
        "SELECT * FROM uploaded_files WHERE id = ? AND user_id = ?",
        (file_id, current_user["id"]),
    ).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="Arquivo nao encontrado.")

    path = Path(row["file_path"])
    if path.exists():
        path.unlink()
    db.execute("DELETE FROM uploaded_files WHERE id = ?", (file_id,))
    db.commit()
    return {"file_id": file_id, "deleted": True}
