from collections.abc import Iterator
from pathlib import Path
import sqlite3

from app.config import settings
from app.content_seed import AVATAR_ITEMS, CAMPAIGN_NODES, DAILY_QUESTIONS, SUBJECTS


def _resolve_path(path: Path) -> Path:
    if path.is_absolute():
        return path
    backend_root = Path(__file__).resolve().parents[1]
    return backend_root / path


DATABASE_FILE = _resolve_path(settings.database_path)


def get_connection() -> sqlite3.Connection:
    DATABASE_FILE.parent.mkdir(parents=True, exist_ok=True)
    connection = sqlite3.connect(DATABASE_FILE)
    connection.row_factory = sqlite3.Row
    connection.execute("PRAGMA foreign_keys = ON")
    return connection


def get_db() -> Iterator[sqlite3.Connection]:
    connection = get_connection()
    try:
        yield connection
    finally:
        connection.close()


def init_db() -> None:
    with get_connection() as db:
        db.executescript(
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS chat_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                title TEXT NOT NULL DEFAULT 'Nova conversa',
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id INTEGER NOT NULL,
                user_id INTEGER,
                sender TEXT NOT NULL CHECK (sender IN ('user', 'ai', 'system')),
                content TEXT NOT NULL,
                subject TEXT,
                level TEXT,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
            );

            CREATE TABLE IF NOT EXISTS uploaded_files (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                session_id INTEGER,
                file_name TEXT NOT NULL,
                file_type TEXT,
                file_path TEXT NOT NULL,
                file_size INTEGER NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
                FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE SET NULL
            );

            CREATE TABLE IF NOT EXISTS study_progress (
                user_id INTEGER PRIMARY KEY,
                correct_answers INTEGER NOT NULL DEFAULT 0,
                answered_exercises INTEGER NOT NULL DEFAULT 0,
                completed_phases INTEGER NOT NULL DEFAULT 0,
                total_phases INTEGER NOT NULL DEFAULT 20,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS subjects (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                exam_focus TEXT NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0
            );

            CREATE TABLE IF NOT EXISTS lessons (
                id TEXT PRIMARY KEY,
                subject_id TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                content TEXT NOT NULL,
                exam_tags TEXT NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS lesson_progress (
                user_id INTEGER NOT NULL,
                lesson_id TEXT NOT NULL,
                completed_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (user_id, lesson_id),
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS daily_questions (
                id TEXT PRIMARY KEY,
                question TEXT NOT NULL,
                options_json TEXT NOT NULL,
                correct_index INTEGER NOT NULL,
                explanation TEXT NOT NULL,
                subject_id TEXT NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS quiz_attempts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                score INTEGER NOT NULL,
                total INTEGER NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
            );

            CREATE TABLE IF NOT EXISTS campaign_nodes (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                subject_id TEXT NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS campaign_progress (
                user_id INTEGER NOT NULL,
                node_id TEXT NOT NULL,
                score INTEGER NOT NULL DEFAULT 0,
                total INTEGER NOT NULL DEFAULT 0,
                completed_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (user_id, node_id),
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (node_id) REFERENCES campaign_nodes(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS avatar_items (
                id TEXT PRIMARY KEY,
                category TEXT NOT NULL,
                name TEXT NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0
            );

            CREATE TABLE IF NOT EXISTS analytics_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                event_type TEXT NOT NULL,
                topic TEXT NOT NULL DEFAULT 'Fisica',
                is_correct INTEGER,
                difficulty TEXT,
                response_time_seconds INTEGER NOT NULL DEFAULT 0,
                time_spent_seconds INTEGER NOT NULL DEFAULT 0,
                metadata_json TEXT NOT NULL DEFAULT '{}',
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS study_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                topic TEXT NOT NULL DEFAULT 'Fisica',
                started_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                completed_at TEXT,
                duration_seconds INTEGER NOT NULL DEFAULT 0,
                is_completed INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );

            CREATE INDEX IF NOT EXISTS idx_messages_session_id ON messages(session_id);
            CREATE INDEX IF NOT EXISTS idx_messages_user_id ON messages(user_id);
            CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON chat_sessions(user_id);
            CREATE INDEX IF NOT EXISTS idx_files_user_id ON uploaded_files(user_id);
            CREATE INDEX IF NOT EXISTS idx_lessons_subject_id ON lessons(subject_id);
            CREATE INDEX IF NOT EXISTS idx_lesson_progress_user_id ON lesson_progress(user_id);
            CREATE INDEX IF NOT EXISTS idx_daily_questions_subject_id ON daily_questions(subject_id);
            CREATE INDEX IF NOT EXISTS idx_quiz_attempts_user_created ON quiz_attempts(user_id, created_at);
            CREATE INDEX IF NOT EXISTS idx_campaign_nodes_subject_id ON campaign_nodes(subject_id);
            CREATE INDEX IF NOT EXISTS idx_campaign_progress_user_id ON campaign_progress(user_id);
            CREATE INDEX IF NOT EXISTS idx_avatar_items_category ON avatar_items(category);
            CREATE INDEX IF NOT EXISTS idx_analytics_user_created ON analytics_events(user_id, created_at);
            CREATE INDEX IF NOT EXISTS idx_analytics_user_topic ON analytics_events(user_id, topic);
            CREATE INDEX IF NOT EXISTS idx_sessions_user_started ON study_sessions(user_id, started_at);
            """
        )
        _ensure_user_settings_columns(db)
        _ensure_learning_columns(db)
        _seed_content(db)


def _ensure_user_settings_columns(db: sqlite3.Connection) -> None:
    columns = {row["name"] for row in db.execute("PRAGMA table_info(users)").fetchall()}
    migrations = {
        "phone": "ALTER TABLE users ADD COLUMN phone TEXT",
        "private_account": "ALTER TABLE users ADD COLUMN private_account INTEGER NOT NULL DEFAULT 0",
        "notifications_enabled": "ALTER TABLE users ADD COLUMN notifications_enabled INTEGER NOT NULL DEFAULT 1",
    }
    for column, statement in migrations.items():
        if column not in columns:
            db.execute(statement)


def _ensure_learning_columns(db: sqlite3.Connection) -> None:
    columns = {row["name"] for row in db.execute("PRAGMA table_info(daily_questions)").fetchall()}
    migrations = {
        "difficulty": "ALTER TABLE daily_questions ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'Media'",
        "topic": "ALTER TABLE daily_questions ADD COLUMN topic TEXT NOT NULL DEFAULT 'Fisica'",
    }
    for column, statement in migrations.items():
        if column not in columns:
            db.execute(statement)


def _seed_content(db: sqlite3.Connection) -> None:
    for subject in SUBJECTS:
        db.execute(
            """
            INSERT INTO subjects (id, name, description, exam_focus, sort_order)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                name = excluded.name,
                description = excluded.description,
                exam_focus = excluded.exam_focus,
                sort_order = excluded.sort_order
            """,
            (
                subject["id"],
                subject["name"],
                subject["description"],
                subject["exam_focus"],
                subject["sort_order"],
            ),
        )
        for lesson in subject["lessons"]:
            db.execute(
                """
                INSERT INTO lessons (id, subject_id, title, description, content, exam_tags, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    subject_id = excluded.subject_id,
                    title = excluded.title,
                    description = excluded.description,
                    content = excluded.content,
                    exam_tags = excluded.exam_tags,
                    sort_order = excluded.sort_order
                """,
                (
                    lesson["id"],
                    subject["id"],
                    lesson["title"],
                    lesson["description"],
                    lesson["content"],
                    lesson["exam_tags"],
                    lesson["sort_order"],
                ),
            )

    import json

    for question in DAILY_QUESTIONS:
        db.execute(
            """
            INSERT INTO daily_questions (
                id, question, options_json, correct_index, explanation,
                subject_id, sort_order, difficulty, topic
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                question = excluded.question,
                options_json = excluded.options_json,
                correct_index = excluded.correct_index,
                explanation = excluded.explanation,
                subject_id = excluded.subject_id,
                sort_order = excluded.sort_order,
                difficulty = excluded.difficulty,
                topic = excluded.topic
            """,
            (
                question["id"],
                question["question"],
                json.dumps(question["options"], ensure_ascii=False),
                question["correct_index"],
                question["explanation"],
                question["subject_id"],
                question["sort_order"],
                question.get("difficulty", "Media"),
                question.get("topic", question["subject_id"].replace("-", " ").title()),
            ),
        )

    for node in CAMPAIGN_NODES:
        db.execute(
            """
            INSERT INTO campaign_nodes (id, title, description, subject_id, sort_order)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                title = excluded.title,
                description = excluded.description,
                subject_id = excluded.subject_id,
                sort_order = excluded.sort_order
            """,
            (node["id"], node["title"], node["description"], node["subject_id"], node["sort_order"]),
        )

    for item in AVATAR_ITEMS:
        db.execute(
            """
            INSERT INTO avatar_items (id, category, name, sort_order)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                category = excluded.category,
                name = excluded.name,
                sort_order = excluded.sort_order
            """,
            (item["id"], item["category"], item["name"], item["sort_order"]),
        )
