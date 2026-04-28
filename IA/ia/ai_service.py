import os
import base64
from openai import OpenAI
from dotenv import load_dotenv
from ia.doc_reader import extrair_texto_documento

from ia.prompt import PROMPT_FISICA
from ia.mock_ai import responder_mock

load_dotenv()

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
USE_MOCK_AI = os.getenv("USE_MOCK_AI", "true").lower() == "true"
MODEL_NAME = os.getenv("MODEL_NAME", "gpt-4.1-mini")


def imagem_para_base64(caminho_imagem: str) -> str:
    with open(caminho_imagem, "rb") as arquivo:
        return base64.b64encode(arquivo.read()).decode("utf-8")


def responder_texto(pergunta: str) -> str:
    if not pergunta or not pergunta.strip():
        return "Erro: a pergunta não pode estar vazia."

    if USE_MOCK_AI:
        return responder_mock(pergunta)

    if not OPENAI_API_KEY:
        return "Erro: OPENAI_API_KEY não encontrada no arquivo .env."

    try:
        client = OpenAI(api_key=OPENAI_API_KEY)

        resposta = client.responses.create(
            model=MODEL_NAME,
            input=[
                {"role": "system", "content": PROMPT_FISICA},
                {"role": "user", "content": pergunta}
            ]
        )

        return resposta.output_text

    except Exception as erro:
        mensagem = str(erro)

        if "insufficient_quota" in mensagem:
            return (
                "Erro: sua conta da OpenAI API está sem crédito/cota disponível. "
                "Use USE_MOCK_AI=true no .env para testar sem gastar crédito."
            )

        return f"Erro ao consultar IA: {erro}"


def responder_com_imagem(pergunta: str, caminho_imagem: str) -> str:
    if USE_MOCK_AI:
        return f"""
IA Física - MODO SIMULADO COM IMAGEM

Pergunta recebida:
{pergunta}

Imagem recebida:
{caminho_imagem}

Resposta simulada:
A imagem seria analisada pela IA para identificar enunciado, fórmulas, gráficos,
tabelas ou desenhos de Física.
"""

    if not OPENAI_API_KEY:
        return "Erro: OPENAI_API_KEY não encontrada no arquivo .env."

    try:
        client = OpenAI(api_key=OPENAI_API_KEY)

        imagem_base64 = imagem_para_base64(caminho_imagem)

        resposta = client.responses.create(
            model=MODEL_NAME,
            input=[
                {
                    "role": "system",
                    "content": PROMPT_FISICA
                },
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "input_text",
                            "text": pergunta
                        },
                        {
                            "type": "input_image",
                            "image_url": f"data:image/jpeg;base64,{imagem_base64}"
                        }
                    ]
                }
            ]
        )

        return resposta.output_text

    except Exception as erro:
        mensagem = str(erro)

        if "insufficient_quota" in mensagem:
            return (
                "Erro: sua conta da OpenAI API está sem crédito/cota disponível. "
                "Use USE_MOCK_AI=true para testar sem gastar crédito."
            )

        return f"Erro ao analisar imagem: {erro}"
    
def responder_com_documento(pergunta: str, caminho_documento: str) -> str:
    texto_documento = extrair_texto_documento(caminho_documento)

    if texto_documento.startswith("ERRO:"):
        return texto_documento

    if USE_MOCK_AI:
        return f"""
IA Física - MODO SIMULADO COM DOCUMENTO

Pergunta recebida:
{pergunta}

Documento recebido:
{caminho_documento}

Trecho inicial extraído do documento:
{texto_documento[:1000]}

Resposta simulada:
A IA usaria o conteúdo do documento para responder à pergunta do aluno.
"""

    if not OPENAI_API_KEY:
        return "Erro: OPENAI_API_KEY não encontrada no arquivo .env."

    try:
        client = OpenAI(api_key=OPENAI_API_KEY)

        prompt_documento = f"""
Use o documento abaixo como fonte de apoio para responder à pergunta do aluno.

DOCUMENTO:
{texto_documento[:12000]}

PERGUNTA DO ALUNO:
{pergunta}
"""

        resposta = client.responses.create(
            model=MODEL_NAME,
            input=[
                {"role": "system", "content": PROMPT_FISICA},
                {"role": "user", "content": prompt_documento}
            ]
        )

        return resposta.output_text

    except Exception as erro:
        mensagem = str(erro)

        if "insufficient_quota" in mensagem:
            return (
                "Erro: sua conta da OpenAI API está sem crédito/cota disponível. "
                "Use USE_MOCK_AI=true para testar sem gastar crédito."
            )

        return f"Erro ao consultar documento com IA: {erro}"