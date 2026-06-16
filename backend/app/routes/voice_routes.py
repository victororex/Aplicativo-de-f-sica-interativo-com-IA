import logging

from fastapi import APIRouter

from tts.voice_service import voice_service

logger = logging.getLogger(__name__)
router = APIRouter()


@router.get("/status")
def voice_status() -> dict:
    """Health check do pipeline de voz personalizada.

    Cada chave indica qual componente está disponível. Útil para o Android
    mostrar status e para diagnóstico operacional.
    """
    status = voice_service.status()
    logger.info("[VOICE] status request: %s", status)
    return status
