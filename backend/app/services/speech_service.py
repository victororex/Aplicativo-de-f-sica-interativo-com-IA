from pathlib import Path
import tempfile

from openai import OpenAI

from app.config import settings


VOICE_INSTRUCTIONS = (
    "Fale em portugues brasileiro, como um professor particular calmo e presente. "
    "Use ritmo natural, calor humano, boa diccao, pausas perceptiveis entre ideias "
    "e entonacao de conversa. Nao soe como leitura robotica."
)


def generate_human_speech(text: str) -> bytes:
    cleaned = _prepare_text_for_voice(text)
    if not cleaned:
        raise ValueError("Texto vazio.")
    if not settings.openai_api_key:
        raise RuntimeError("Voz humana nao configurada.")

    client = OpenAI(api_key=settings.openai_api_key)
    speech_path: Path | None = None

    try:
        with tempfile.NamedTemporaryFile(suffix=".mp3", delete=False) as temp_file:
            speech_path = Path(temp_file.name)

        with client.audio.speech.with_streaming_response.create(
            model=settings.tts_model,
            voice=settings.tts_voice,
            input=cleaned[:4000],
            instructions=VOICE_INSTRUCTIONS,
        ) as response:
            response.stream_to_file(speech_path)

        return speech_path.read_bytes()
    finally:
        if speech_path and speech_path.exists():
            speech_path.unlink()


def _prepare_text_for_voice(text: str) -> str:
    return (
        text.replace("Resposta simulada:", "")
        .replace("Dados do problema:", "Vamos pelos dados do problema.")
        .replace("Formula usada:", "A formula usada e:")
        .replace("Fórmula usada:", "A formula usada e:")
        .replace("Observacao:", "Observacao:")
        .replace("Observação:", "Observacao:")
        .replace("P = W / t", "Potencia e igual ao trabalho dividido pelo tempo.")
        .replace("W = F · d", "Trabalho e igual a forca vezes a distancia.")
        .strip()
    )
