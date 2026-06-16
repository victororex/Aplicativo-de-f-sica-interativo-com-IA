import logging

from tts.voice_service import VoiceCloneUnavailable, voice_service


logger = logging.getLogger(__name__)


def generate_human_speech(text: str) -> bytes:
    """Generate speech with the cached professor voice pipeline."""
    try:
        return voice_service.synthesize(text)
    except VoiceCloneUnavailable as error:
        logger.exception("Voice cloning unavailable")
        detail = str(error).strip() or "Voz do professor indisponivel no momento."
        raise RuntimeError(detail) from None
