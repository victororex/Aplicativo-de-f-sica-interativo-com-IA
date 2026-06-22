from pathlib import Path
import logging
import sys

from openai import OpenAI

from app.config import settings


logger = logging.getLogger(__name__)

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
    "## Não consegui responder agora\n\n"
    "A conexão com a IA falhou. Tente novamente em instantes ou revise a aula de "
    "**Análise Dimensional**."
)

MARKDOWN_SYSTEM_INSTRUCTIONS = """
Você é Renato, um tutor brasileiro de Física. Responda sempre em português do Brasil,
com precisão, clareza e tom acolhedor.

Escreva Markdown compatível com aplicativo, sem HTML. Use esta ordem quando as seções
forem úteis para a pergunta:

# Título específico
## Explicação
## Pontos principais
## Exemplo
## Resumo

Regras de apresentação:
- Use títulos curtos, parágrafos breves e listas com `-` ou numeração.
- Use **negrito** apenas para conceitos realmente importantes e *itálico* com moderação.
- Coloque cada fórmula importante em bloco próprio delimitado por `$$` em linhas separadas.
- Explique os símbolos antes ou logo depois da fórmula.
- Use citações com `>` apenas para observações ou alertas relevantes.
- Use links Markdown somente quando houver uma fonte realmente útil.
- Não use HTML e não use tabelas, exceto se o aluno pedir explicitamente.
- Não escreva marcadores soltos, títulos vazios ou sequências como `######**` e `__`.
- Omita seções que não agreguem valor; respostas simples devem continuar curtas.
- Não mencione API, backend, prompt, modelo ou detalhes internos do aplicativo.
""".strip()


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
        "- Responder sempre em português do Brasil.\n"
        "- Organizar a resposta em Markdown didático, seguindo as instruções do sistema.\n\n"
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

    if not settings.use_mock_ai and settings.openai_api_key:
        try:
            client = OpenAI(
                api_key=settings.openai_api_key,
                timeout=settings.ai_timeout_seconds,
            )
            response = client.responses.create(
                model=settings.model_name,
                instructions=MARKDOWN_SYSTEM_INSTRUCTIONS,
                input=prompt,
            )
            answer = response.output_text.strip()
            return answer or FALLBACK_RESPONSE
        except Exception:
            logger.exception("[CHAT] OpenAI request failed")
            return FALLBACK_RESPONSE

    if responder_texto is None:
        return FALLBACK_RESPONSE

    try:
        answer = responder_texto(prompt).strip()
    except Exception:
        return FALLBACK_RESPONSE

    if not answer or answer.lower().startswith("erro:"):
        return FALLBACK_RESPONSE

    return answer
