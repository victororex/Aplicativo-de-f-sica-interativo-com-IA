import sqlite3

from fastapi import APIRouter, Depends, HTTPException, status

from app.config import settings
from app.database import get_db
from app.schemas.auth_schema import AuthResponse, LoginRequest, RegisterRequest, UserResponse
from app.security import create_access_token, get_current_user, hash_password, verify_password

router = APIRouter()


def _normalize_email(email: str) -> str:
    return email.strip().lower()


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


@router.post("/register", response_model=AuthResponse, status_code=status.HTTP_201_CREATED)
def register(request: RegisterRequest, db: sqlite3.Connection = Depends(get_db)):
    email = _normalize_email(request.email)
    if "@" not in email or "." not in email.rsplit("@", 1)[-1]:
        raise HTTPException(status_code=422, detail="Informe um e-mail valido.")

    try:
        cursor = db.execute(
            "INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)",
            (request.name.strip(), email, hash_password(request.password)),
        )
        db.execute(
            "INSERT INTO study_progress (user_id, total_phases) VALUES (?, (SELECT COUNT(*) FROM lessons))",
            (cursor.lastrowid,),
        )
        db.commit()
    except sqlite3.IntegrityError as error:
        raise HTTPException(status_code=409, detail="Este e-mail ja esta cadastrado.") from error

    user = db.execute("SELECT * FROM users WHERE id = ?", (cursor.lastrowid,)).fetchone()
    token = create_access_token(user_id=user["id"], email=user["email"])
    return AuthResponse(
        access_token=token,
        expires_in_minutes=settings.jwt_expire_minutes,
        user=_user_response(user),
    )


@router.post("/login", response_model=AuthResponse)
def login(request: LoginRequest, db: sqlite3.Connection = Depends(get_db)):
    email = _normalize_email(request.email)
    user = db.execute("SELECT * FROM users WHERE email = ?", (email,)).fetchone()
    if user is None or not verify_password(request.password, user["password_hash"]):
        raise HTTPException(status_code=401, detail="E-mail ou senha invalidos.")

    token = create_access_token(user_id=user["id"], email=user["email"])
    return AuthResponse(
        access_token=token,
        expires_in_minutes=settings.jwt_expire_minutes,
        user=_user_response(user),
    )


@router.get("/me", response_model=UserResponse)
def me(current_user: sqlite3.Row = Depends(get_current_user)):
    return _user_response(current_user)
