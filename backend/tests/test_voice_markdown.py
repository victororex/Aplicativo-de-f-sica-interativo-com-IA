from app.services.ai_service import MARKDOWN_SYSTEM_INSTRUCTIONS
from tts.voice_service import prepare_text_for_voice


def test_system_prompt_requires_clean_markdown() -> None:
    assert "# Título específico" in MARKDOWN_SYSTEM_INSTRUCTIONS
    assert "Não use HTML" in MARKDOWN_SYSTEM_INSTRUCTIONS
    assert "não use tabelas" in MARKDOWN_SYSTEM_INSTRUCTIONS


def test_voice_text_has_no_markdown_control_characters() -> None:
    spoken = prepare_text_for_voice(
        "# Velocidade\n\n"
        "- Use **distância** e *tempo*.\n\n"
        "$$\n[v] = [L][T]^-1\n$$\n\n"
        "[Saiba mais](https://example.com)"
    )

    assert spoken == (
        "Velocidade Use distância e tempo. "
        "[v] = [L][T]^-1 Saiba mais"
    )
