from dataclasses import dataclass
from pathlib import Path
import os

from dotenv import load_dotenv


load_dotenv()


def _split_csv(value: str) -> list[str]:
    return [item.strip() for item in value.split(",") if item.strip()]


@dataclass(frozen=True)
class Settings:
    app_name: str = os.getenv("APP_NAME", "API - Aplicativo de Fisica com IA")
    app_env: str = os.getenv("APP_ENV", "development")
    app_debug: bool = os.getenv("APP_DEBUG", "false").lower() == "true"
    api_version: str = os.getenv("API_VERSION", "1.0.0")
    database_path: Path = Path(os.getenv("DATABASE_PATH", "data/fisica_ia.sqlite3"))
    upload_dir: Path = Path(os.getenv("UPLOAD_DIR", "data/uploads"))
    jwt_secret_key: str = os.getenv("JWT_SECRET_KEY", "troque-esta-chave-em-producao")
    jwt_expire_minutes: int = int(os.getenv("JWT_EXPIRE_MINUTES", "120"))
    allowed_origins: list[str] = None  # type: ignore[assignment]
    max_upload_mb: int = int(os.getenv("MAX_UPLOAD_MB", "10"))
    openai_api_key: str | None = os.getenv("OPENAI_API_KEY")
    tts_model: str = os.getenv("TTS_MODEL", "gpt-4o-mini-tts")
    tts_voice: str = os.getenv("TTS_VOICE", "marin")

    def __post_init__(self) -> None:
        origins = os.getenv(
            "ALLOWED_ORIGINS",
            "http://localhost:3000,http://127.0.0.1:3000,http://10.0.2.2:8000",
        )
        object.__setattr__(self, "allowed_origins", _split_csv(origins))

    @property
    def max_upload_bytes(self) -> int:
        return self.max_upload_mb * 1024 * 1024

    @property
    def is_production(self) -> bool:
        return self.app_env.lower() == "production"


settings = Settings()
