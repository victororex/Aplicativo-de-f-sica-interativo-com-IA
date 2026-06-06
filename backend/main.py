from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import settings
from app.database import init_db
from app.routes import auth_routes, chat_routes, content_routes, file_routes, history_routes, learning_routes, progress_routes, stats_routes, user_routes

app = FastAPI(
    title=settings.app_name,
    description="Back-end responsavel por conectar o app mobile, a IA e os dados do usuario.",
    version=settings.api_version,
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
app.include_router(stats_routes.router, prefix="/stats", tags=["Estatisticas"])
app.include_router(content_routes.router, prefix="/content", tags=["Conteudo"])
app.include_router(progress_routes.router, prefix="/progress", tags=["Progresso"])
app.include_router(learning_routes.router, prefix="/learning", tags=["Aprendizagem"])


@app.on_event("startup")
def startup() -> None:
    init_db()


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
