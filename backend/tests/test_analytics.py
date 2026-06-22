from datetime import datetime
import sqlite3

from app.services.stats_service import get_learning_dashboard


def analytics_db() -> sqlite3.Connection:
    db = sqlite3.connect(":memory:")
    db.row_factory = sqlite3.Row
    db.executescript(
        """
        CREATE TABLE analytics_events (
            id INTEGER PRIMARY KEY,
            user_id INTEGER,
            event_type TEXT,
            topic TEXT,
            is_correct INTEGER,
            difficulty TEXT,
            response_time_seconds INTEGER DEFAULT 0,
            time_spent_seconds INTEGER DEFAULT 0,
            created_at TEXT
        );
        CREATE TABLE study_sessions (
            id INTEGER PRIMARY KEY,
            user_id INTEGER,
            duration_seconds INTEGER,
            is_completed INTEGER,
            started_at TEXT
        );
        CREATE TABLE messages (id INTEGER PRIMARY KEY, user_id INTEGER, sender TEXT);
        CREATE TABLE lessons (id TEXT PRIMARY KEY);
        CREATE TABLE lesson_progress (user_id INTEGER, lesson_id TEXT);
        CREATE TABLE study_progress (
            user_id INTEGER PRIMARY KEY,
            correct_answers INTEGER,
            answered_exercises INTEGER,
            completed_phases INTEGER,
            total_phases INTEGER
        );
        """
    )
    return db


def test_dashboard_detects_weak_topic_and_recommends_review():
    db = analytics_db()
    for index, correct in enumerate((0, 0, 1, 0), start=1):
        db.execute(
            """
            INSERT INTO analytics_events VALUES (?, 7, 'exercise_answered', 'Dinamica', ?, 'Media', 70, 70, ?)
            """,
            (index, correct, f"2026-06-{10 + index:02d} 10:00:00"),
        )
    db.execute(
        "INSERT INTO study_sessions VALUES (1, 7, 900, 1, '2026-06-14 09:00:00')"
    )
    db.commit()

    dashboard = get_learning_dashboard(db, 7, now=datetime(2026, 6, 15, 12, 0))

    assert dashboard["questions_answered"] == 4
    assert dashboard["accuracy_rate"] == 25
    assert dashboard["adaptive_profile"]["exercise_difficulty"] in {"Muito Fácil", "Fácil"}
    assert 0 <= dashboard["adaptive_profile"]["fuzzy_score"] <= 100
    assert dashboard["adaptive_profile"]["next_topic"] == "Dinamica"
    assert dashboard["difficult_topics"][0]["needs_review"] is True
    assert dashboard["completed_sessions"] == 1


def test_dashboard_preserves_empty_state_contract():
    db = analytics_db()
    dashboard = get_learning_dashboard(db, None, now=datetime(2026, 6, 15, 12, 0))

    assert dashboard["questions_answered"] == 0
    assert len(dashboard["daily_evolution"]) == 7
    assert len(dashboard["weekly_evolution"]) == 6
    assert len(dashboard["monthly_evolution"]) == 6
    assert dashboard["recommendations"][0]["title"] == "Crie sua linha de base"
    assert dashboard["ocr_uses"] == 0
    assert dashboard["voice_uses"] == 0


def test_chat_activity_populates_history_and_evolution_without_fake_mastery():
    db = analytics_db()
    db.execute(
        """
        INSERT INTO analytics_events VALUES
        (1, 7, 'chat_question_sent', 'Cinematica', NULL, NULL, 2, 0, '2026-06-15 10:00:00')
        """
    )
    db.execute("INSERT INTO messages VALUES (1, 7, 'user')")
    db.commit()

    dashboard = get_learning_dashboard(db, 7, now=datetime(2026, 6, 15, 12, 0))

    assert dashboard["daily_evolution"][-1]["activities"] == 1
    assert dashboard["performance_history"][0]["activity"] == "Chat com Renato"
    assert dashboard["topic_performance"] == []
    assert dashboard["accuracy_rate"] == 0
