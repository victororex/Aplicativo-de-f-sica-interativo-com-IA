import sqlite3

from fastapi import APIRouter, Depends, HTTPException

from app.database import get_db
from app.schemas.auth_schema import UpdateUserRequest, UserResponse
from app.security import get_current_user

router = APIRouter()


def _user_response(row: sqlite3.Row) -> UserResponse:
    return UserResponse(
        id=row["id"],
        name=row["name"],
        email=row["email"],
        phone=row["phone"],
        private_account=bool(row["private_account"]),
        notifications_enabled=bool(row["notifications_enabled"]),
        created_at=row["created_at"],
    )


@router.get("/me", response_model=UserResponse)
def get_me(current_user: sqlite3.Row = Depends(get_current_user)):
    return _user_response(current_user)


@router.put("/me", response_model=UserResponse)
def update_me(
    request: UpdateUserRequest,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row = Depends(get_current_user),
):
    name = request.name.strip() if request.name else current_user["name"]
    email = request.email.strip().lower() if request.email else current_user["email"]
    phone = request.phone.strip() if request.phone is not None and request.phone.strip() else None
    if request.phone is None:
        phone = current_user["phone"]
    private_account = (
        int(request.private_account)
        if request.private_account is not None
        else int(current_user["private_account"])
    )
    notifications_enabled = (
        int(request.notifications_enabled)
        if request.notifications_enabled is not None
        else int(current_user["notifications_enabled"])
    )
    if "@" not in email or "." not in email.rsplit("@", 1)[-1]:
        raise HTTPException(status_code=422, detail="Informe um e-mail valido.")

    try:
        db.execute(
            """
            UPDATE users
            SET name = ?,
                email = ?,
                phone = ?,
                private_account = ?,
                notifications_enabled = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            (name, email, phone, private_account, notifications_enabled, current_user["id"]),
        )
        db.commit()
    except sqlite3.IntegrityError as error:
        raise HTTPException(status_code=409, detail="Este e-mail ja esta em uso.") from error

    updated = db.execute("SELECT * FROM users WHERE id = ?", (current_user["id"],)).fetchone()
    return _user_response(updated)
