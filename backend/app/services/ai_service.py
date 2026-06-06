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


def generate_physics_response(user_message: str, level: str = "universitario", subject: str | None = None) -> str:
    if not user_message.strip():
        return "Erro: a pergunta nao pode estar vazia."

    prompt = f"Nivel do aluno: {level}\n"
    if subject:
        prompt += f"Assunto: {subject}\n"
    prompt += f"Pergunta: {user_message}"

    if responder_texto is None:
        return f"Erro ao carregar modulo de IA: {import_error}"

    return responder_texto(prompt)
