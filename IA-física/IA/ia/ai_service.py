import base64
import os

from dotenv import load_dotenv
from openai import OpenAI

from ia.doc_reader import extrair_texto_documento
from ia.mock_ai import responder_mock
from ia.prompt import PROMPT_FISICA

load_dotenv()

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
USE_MOCK_AI = os.getenv("USE_MOCK_AI", "true").lower() == "true"
MODEL_NAME = os.getenv("MODEL_NAME", "gpt-4o-mini")
AI_TIMEOUT_SECONDS = float(os.getenv("AI_TIMEOUT_SECONDS", "30"))

SAFE_FAILURE = (
    "Nao consegui responder agora porque a conexao com a IA falhou. "
    "Tente novamente em instantes ou revise a aula de Analise Dimensional."
)


def imagem_para_base64(caminho_imagem: str) -> str:
    with open(caminho_imagem, "rb") as arquivo:
        return base64.b64encode(arquivo.read()).decode("utf-8")


def responder_texto(pergunta: str) -> str:
    if not pergunta or not pergunta.strip():
        return "Envie uma pergunta sobre Fisica para eu poder ajudar."

    if USE_MOCK_AI:
        return responder_mock(pergunta)

    if not OPENAI_API_KEY:
        return SAFE_FAILURE

    try:
        client = OpenAI(api_key=OPENAI_API_KEY, timeout=AI_TIMEOUT_SECONDS)
        resposta = client.responses.create(
            model=MODEL_NAME,
            input=[
                {"role": "system", "content": PROMPT_FISICA},
                {"role": "user", "content": pergunta},
            ],
        )
        return resposta.output_text.strip() or SAFE_FAILURE
    except Exception:
        return SAFE_FAILURE


def responder_com_imagem(pergunta: str, caminho_imagem: str) -> str:
    if USE_MOCK_AI:
        return """
Resposta curta:
Recebi sua pergunta com imagem e posso ajudar a interpretar enunciados, formulas, graficos ou diagramas de Fisica.

Passo a passo:
* Observe as grandezas no desenho ou enunciado.
* Separe unidades, valores e relacoes.
* Compare as dimensoes fisicas envolvidas.

Resumo final:
Use a imagem como apoio para identificar quais grandezas entram na analise dimensional.
"""

    if not OPENAI_API_KEY:
        return SAFE_FAILURE

    try:
        client = OpenAI(api_key=OPENAI_API_KEY, timeout=AI_TIMEOUT_SECONDS)
        imagem_base64 = imagem_para_base64(caminho_imagem)
        resposta = client.responses.create(
            model=MODEL_NAME,
            input=[
                {"role": "system", "content": PROMPT_FISICA},
                {
                    "role": "user",
                    "content": [
                        {"type": "input_text", "text": pergunta},
                        {"type": "input_image", "image_url": f"data:image/jpeg;base64,{imagem_base64}"},
                    ],
                },
            ],
        )
        return resposta.output_text.strip() or SAFE_FAILURE
    except Exception:
        return SAFE_FAILURE


def responder_com_documento(pergunta: str, caminho_documento: str) -> str:
    texto_documento = extrair_texto_documento(caminho_documento)

    if texto_documento.startswith("ERRO:"):
        return "Nao consegui ler o documento agora. Tente novamente com outro arquivo."

    if USE_MOCK_AI:
        return """
Resposta curta:
Recebi sua pergunta com documento e posso ajudar a relacionar o conteudo com Fisica.

Passo a passo:
* Leia o enunciado procurando grandezas e unidades.
* Identifique formulas citadas.
* Confira se as dimensoes dos dois lados combinam.

Resumo final:
O documento deve servir como fonte de apoio para revisar conceitos e resolver a questao com calma.
"""

    if not OPENAI_API_KEY:
        return SAFE_FAILURE

    try:
        client = OpenAI(api_key=OPENAI_API_KEY, timeout=AI_TIMEOUT_SECONDS)
        prompt_documento = f"""
Use o documento abaixo como fonte de apoio para responder a pergunta do aluno.

DOCUMENTO:
{texto_documento[:12000]}

PERGUNTA DO ALUNO:
{pergunta}
"""
        resposta = client.responses.create(
            model=MODEL_NAME,
            input=[
                {"role": "system", "content": PROMPT_FISICA},
                {"role": "user", "content": prompt_documento},
            ],
        )
        return resposta.output_text.strip() or SAFE_FAILURE
    except Exception:
        return SAFE_FAILURE
