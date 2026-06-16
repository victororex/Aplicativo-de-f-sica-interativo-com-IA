from importlib.util import find_spec
from pathlib import Path
import sys

import soundfile


BACKEND_DIR = Path(__file__).resolve().parents[1]
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))


def require(condition: bool, message: str) -> None:
    if not condition:
        print(f"ERROR: {message}", file=sys.stderr)
        raise SystemExit(1)


def main() -> None:
    required_modules = ("fastapi", "openai", "PIL", "sympy", "melo", "openvoice")
    missing = [module for module in required_modules if find_spec(module) is None]
    require(not missing, f"Pacotes ausentes: {', '.join(missing)}")

    require(
        (BACKEND_DIR / "checkpoints_v2" / "converter" / "config.json").exists(),
        "checkpoints_v2/converter/config.json nao encontrado.",
    )
    require(
        (BACKEND_DIR / "checkpoints_v2" / "converter" / "checkpoint.pth").exists(),
        "checkpoints_v2/converter/checkpoint.pth nao encontrado.",
    )

    reference = BACKEND_DIR / "voices" / "professor.wav"
    require(reference.exists(), "voices/professor.wav nao encontrado.")
    info = soundfile.info(reference)
    require(info.duration >= 3, "voices/professor.wav precisa ter pelo menos 3 segundos.")

    from app.database import get_connection, init_db

    init_db()
    with get_connection() as db:
        tables = {
            row["name"]
            for row in db.execute("SELECT name FROM sqlite_master WHERE type = 'table'").fetchall()
        }
        missing_tables = {"analytics_events", "study_sessions"} - tables
        require(not missing_tables, f"Tabelas de analytics ausentes: {sorted(missing_tables)}")

    print("OK: backend, OCR, voz e analytics adaptativo estao prontos.")


if __name__ == "__main__":
    main()
