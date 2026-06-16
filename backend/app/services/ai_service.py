from pathlib import Path
import sys


def _register_existing_ai_module() -> None:
    repo_root = Path(__file__).resolve().parents[3]
    candidates = [
        repo_root / "IA-fisica" / "IA",
        repo_root / "IA-física" / "IA",
    ]

    for ai_path in candidates:
        if ai_path.exists() and str(ai_path) not in sys.path:
            sys.path.insert(0, str(ai_path))
            return


_register_existing_ai_module()

try:
    from ia.ai_service import responder_texto
except Exception as error:
    responder_texto = None
    import_error = error
else:
    import_error = None


FALLBACK_RESPONSE = (
    "Nao consegui responder agora porque a conexao com a IA falhou. "
    "Tente novamente em instantes ou revise a aula de Analise Dimensional."
)


def generate_physics_response(
    user_message: str,
    level: str = "universitario",
    subject: str | None = None,
    context_title: str | None = None,
    context_topic: str | None = None,
    source: str | None = None,
) -> str:
    if not user_message.strip():
        return "Envie uma pergunta sobre Fisica para eu poder ajudar."

    prompt = (
        "Contexto do aplicativo:\n"
        "- Aplicativo educacional brasileiro de Fisica.\n"
        "- Tutor: Titio Renato.\n"
        "- Tema principal atual: Analise Dimensional.\n"
        "- Responder sempre em portugues do Brasil.\n"
        "- Usar formato didatico com resposta curta, passo a passo, formula, exemplo e resumo quando possivel.\n\n"
        f"Nivel do aluno: {level}\n"
    )
    if subject:
        prompt += f"Assunto: {subject}\n"
    if context_title:
        prompt += f"Contexto de tela/aula: {context_title}\n"
    if context_topic:
        prompt += f"Topico relacionado: {context_topic}\n"
    if source:
        prompt += f"Origem da pergunta: {source}\n"
    prompt += f"Pergunta: {user_message}"

    if responder_texto is None:
        return FALLBACK_RESPONSE

    try:
        answer = responder_texto(prompt).strip()
    except Exception:
        return FALLBACK_RESPONSE

    if not answer or answer.lower().startswith("erro:"):
        return FALLBACK_RESPONSE

    return answer
