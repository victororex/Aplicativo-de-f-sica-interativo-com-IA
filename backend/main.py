import logging
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import settings
from app.database import init_db
from app.routes import auth_routes, chat_routes, content_routes, file_routes, formula_routes, history_routes, learning_routes, progress_routes, stats_routes, user_routes, voice_routes

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

_DEFAULT_JWT_SECRET = "troque-esta-chave-em-producao"


@asynccontextmanager
async def lifespan(_: FastAPI):
    _check_security_config()
    init_db()
    _check_voice_assets()
    _warmup_voice_pipeline()
    yield


def _warmup_voice_pipeline() -> None:
    """Tenta inicializar MeloTTS+OpenVoice no startup para falhar cedo."""
    from tts.voice_service import VoiceCloneUnavailable, voice_service
    try:
        voice_service.initialize()
        status = voice_service.status()
        logger.info("[VOICE] pipeline pronto: %s", {k: v for k, v in status.items() if k in ("melotts", "openvoice", "embedding", "device", "audio_generation")})
    except VoiceCloneUnavailable as error:
        logger.warning("[VOICE] pipeline indisponível na inicialização: %s", error)
    except Exception:  # noqa: BLE001
        logger.exception("[VOICE] falha inesperada ao inicializar pipeline")


def _check_security_config() -> None:
    if settings.is_production and settings.jwt_secret_key == _DEFAULT_JWT_SECRET:
        raise RuntimeError(
            "JWT_SECRET_KEY ainda usa o valor padrão — configure uma chave forte antes de subir para produção."
        )
    if settings.is_production and getattr(settings, "use_mock_ai", False):
        logger.warning("[CONFIG] USE_MOCK_AI=true em produção: as respostas de IA serão simuladas.")


def _check_voice_assets() -> None:
    voice_dir = Path(getattr(settings, "voice_reference_dir", "voices"))
    professor = voice_dir / "professor.wav"
    if professor.exists():
        logger.info("[TTS] Reference voice found: %s", professor)
        return
    # Fallback: usar qualquer .wav em voices/
    fallback = next((p for p in voice_dir.glob("*.wav")), None) if voice_dir.exists() else None
    if fallback is not None:
        logger.warning("[TTS] professor.wav ausente; usando fallback %s", fallback)
    else:
        logger.warning("[TTS] voices/professor.wav NOT FOUND — voice cloning will be disabled")

app = FastAPI(
    title=settings.app_name,
    description="Back-end responsavel por conectar o app mobile, a IA e os dados do usuario.",
    version=settings.api_version,
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins if settings.is_production else ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_routes.router, prefix="/auth", tags=["Autenticacao"])
app.include_router(chat_routes.router, prefix="/chat", tags=["Chat"])
app.include_router(user_routes.router, prefix="/users", tags=["Usuarios"])
app.include_router(history_routes.router, prefix="/history", tags=["Historico"])
app.include_router(file_routes.router, prefix="/files", tags=["Arquivos"])
app.include_router(formula_routes.router, prefix="/formula", tags=["OCR de formulas"])
app.include_router(stats_routes.router, prefix="/stats", tags=["Estatisticas"])
app.include_router(content_routes.router, prefix="/content", tags=["Conteudo"])
app.include_router(progress_routes.router, prefix="/progress", tags=["Progresso"])
app.include_router(learning_routes.router, prefix="/learning", tags=["Aprendizagem"])
app.include_router(voice_routes.router, prefix="/voice", tags=["Voz"])

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(_, exc: RequestValidationError):
    return JSONResponse(
        status_code=422,
        content={
            "detail": "Dados invalidos na requisicao.",
            "errors": exc.errors(),
        },
    )


@app.get("/")
def root():
    return {
        "message": "API do Aplicativo de Fisica com IA funcionando.",
        "version": settings.api_version,
        "docs": "/docs",
    }


@app.get("/health")
def health():
    return {"status": "ok", "environment": settings.app_env}
