from __future__ import annotations

import base64
import logging
import math
import re
import time

from openai import OpenAI
from pydantic import BaseModel, Field
import sympy

from app.config import settings
from app.schemas.formula_schema import (
    FormulaAnalysisResponse,
    FormulaGraph,
    FormulaStep,
    GraphPoint,
)
from app.services.image_processing import NormalizedImage


logger = logging.getLogger(__name__)


class FormulaAnalysisUnavailable(RuntimeError):
    """Raised when the configured vision model cannot analyze the image."""


class GraphSpec(BaseModel):
    expression: str = Field(description="Expressao em x, sem o prefixo y=.")
    label: str = "y = f(x)"
    x_min: float = -10.0
    x_max: float = 10.0


class FormulaAnalysisDraft(BaseModel):
    content_type: str = Field(
        default="exercise",
        description="formula, graph, table, diagram, circuit, exercise ou other",
    )
    visual_description: str = ""
    structured_data: list[str] = Field(default_factory=list)
    ocr_text: str
    latex: str
    problem_statement: str
    steps: list[FormulaStep]
    final_answer: str
    graph: GraphSpec | None = None
    narration_text: str
    warnings: list[str] = Field(default_factory=list)


SYSTEM_PROMPT = """
Voce e um especialista em OCR matematico e professor brasileiro de Fisica.
Analise cuidadosamente a foto enviada e devolva apenas a estrutura solicitada.

Regras:
- Classifique content_type como formula, graph, table, diagram, circuit, exercise ou other.
- Descreva em visual_description os elementos visuais relevantes e suas relacoes espaciais.
- Para graficos, extraia eixos, escalas, unidades, pontos e tendencia em structured_data.
- Para tabelas, extraia cabecalhos e linhas legiveis em structured_data.
- Para diagramas e circuitos, identifique componentes, rotulos, conexoes e sentidos.
- Transcreva todo o enunciado legivel em ocr_text.
- Converta a formula principal para LaTeX valido, sem delimitadores Markdown.
- Preserve expoentes, indices, fracoes, radicais, vetores, unidades e sinais.
- Resolva o exercicio em portugues do Brasil, com passos curtos e verificaveis.
- Cada passo deve ter titulo, explicacao e LaTeX opcional.
- Nao invente valores ilegíveis. Registre a duvida em warnings.
- Se houver uma funcao explicita que possa ser desenhada como y=f(x), forneca graph.
- Em graph.expression use sintaxe SymPy simples: +, -, *, /, **, sin, cos, tan, exp,
  log, sqrt, abs e a variavel x. Caso contrario, use null.
- narration_text deve ser uma versao natural e concisa da resolucao para sintese de voz.
- Se a imagem nao contiver formula ou exercicio de Fisica/Matematica, explique isso em
  warnings e ainda devolva os demais campos de forma segura.
""".strip()


def analyze_formula_image(image: NormalizedImage, question: str | None = None) -> FormulaAnalysisResponse:
    started = time.perf_counter()
    if settings.use_mock_ai:
        draft = _mock_analysis()
    else:
        if not settings.openai_api_key:
            raise FormulaAnalysisUnavailable(
                "A analise de formulas requer OPENAI_API_KEY ou USE_MOCK_AI=true."
            )
        draft = _analyze_with_openai(image, question)

    graph = _build_graph(draft.graph)
    response = FormulaAnalysisResponse(
        content_type=draft.content_type.strip().lower() or "other",
        visual_description=draft.visual_description.strip(),
        structured_data=[item.strip() for item in draft.structured_data if item.strip()],
        ocr_text=draft.ocr_text.strip(),
        latex=draft.latex.strip(),
        problem_statement=draft.problem_statement.strip(),
        steps=draft.steps,
        final_answer=draft.final_answer.strip(),
        graph=graph,
        narration_text=draft.narration_text.strip(),
        warnings=draft.warnings,
    )
    logger.info(
        "Formula analysis finished model=%s mock=%s steps=%d graph=%s elapsed=%.2fs",
        settings.ocr_model,
        settings.use_mock_ai,
        len(response.steps),
        response.graph is not None,
        time.perf_counter() - started,
    )
    return response


def _analyze_with_openai(image: NormalizedImage, question: str | None) -> FormulaAnalysisDraft:
    image_base64 = base64.b64encode(image.content).decode("ascii")
    prompt = "Extraia a formula e resolva o exercicio passo a passo."
    if question and question.strip():
        prompt += f"\nPedido adicional do aluno: {question.strip()[:1000]}"

    try:
        client = OpenAI(api_key=settings.openai_api_key, timeout=settings.ai_timeout_seconds)
        response = client.responses.parse(
            model=settings.ocr_model,
            input=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {
                    "role": "user",
                    "content": [
                        {"type": "input_text", "text": prompt},
                        {
                            "type": "input_image",
                            "image_url": f"data:{image.media_type};base64,{image_base64}",
                            "detail": "high",
                        },
                    ],
                },
            ],
            text_format=FormulaAnalysisDraft,
        )
        if response.output_parsed is None:
            raise FormulaAnalysisUnavailable("A IA nao retornou uma analise estruturada.")
        return response.output_parsed
    except FormulaAnalysisUnavailable:
        raise
    except Exception as error:
        logger.exception("OpenAI formula analysis failed")
        raise FormulaAnalysisUnavailable(
            "Nao foi possivel analisar a formula agora. Tente novamente."
        ) from error


def _mock_analysis() -> FormulaAnalysisDraft:
    return FormulaAnalysisDraft(
        content_type="exercise",
        visual_description="Exercicio textual com uma formula de velocidade.",
        structured_data=["d = 100 m", "t = 20 s"],
        ocr_text="Determine a velocidade para d = 100 m e t = 20 s. v = d/t",
        latex=r"v = \frac{d}{t}",
        problem_statement="Calcular a velocidade media para 100 metros percorridos em 20 segundos.",
        steps=[
            FormulaStep(
                title="Identificar os dados",
                explanation="A distancia e 100 m e o tempo e 20 s.",
                latex=r"d = 100\,m,\quad t = 20\,s",
            ),
            FormulaStep(
                title="Aplicar a formula",
                explanation="Velocidade media e distancia dividida pelo tempo.",
                latex=r"v = \frac{100}{20}",
            ),
            FormulaStep(
                title="Calcular",
                explanation="A divisao resulta em 5 metros por segundo.",
                latex=r"v = 5\,m/s",
            ),
        ],
        final_answer="A velocidade media e 5 m/s.",
        graph=GraphSpec(expression="5*x", label="Distancia pelo tempo", x_min=0, x_max=20),
        narration_text=(
            "Temos cem metros percorridos em vinte segundos. "
            "Dividindo a distancia pelo tempo, encontramos cinco metros por segundo."
        ),
    )


_ALLOWED_EXPRESSION = re.compile(r"^[0-9x+\-*/().,\s_a-zA-Z]+$")
_ALLOWED_FUNCTIONS = {
    "sin": sympy.sin,
    "cos": sympy.cos,
    "tan": sympy.tan,
    "exp": sympy.exp,
    "log": sympy.log,
    "sqrt": sympy.sqrt,
    "abs": sympy.Abs,
}


def _build_graph(spec: GraphSpec | None) -> FormulaGraph | None:
    if spec is None:
        return None

    expression_text = spec.expression.strip()
    if expression_text.lower().startswith("y="):
        expression_text = expression_text[2:].strip()
    if not expression_text or len(expression_text) > 200 or not _ALLOWED_EXPRESSION.fullmatch(expression_text):
        logger.warning("Rejected graph expression with unsupported characters")
        return None
    identifiers = set(re.findall(r"[A-Za-z_][A-Za-z0-9_]*", expression_text))
    if not identifiers <= {"x", *_ALLOWED_FUNCTIONS.keys()}:
        logger.warning("Rejected graph expression with unsupported identifiers")
        return None

    x_min = max(-1000.0, min(1000.0, spec.x_min))
    x_max = max(-1000.0, min(1000.0, spec.x_max))
    if x_max <= x_min:
        return None

    x = sympy.Symbol("x")
    try:
        expression = sympy.sympify(expression_text, locals={"x": x, **_ALLOWED_FUNCTIONS})
        if expression.free_symbols - {x}:
            return None
        function = sympy.lambdify(x, expression, modules=["math"])
    except (TypeError, ValueError, SyntaxError, sympy.SympifyError):
        logger.warning("Could not parse graph expression: %s", expression_text)
        return None

    points: list[GraphPoint] = []
    samples = 81
    for index in range(samples):
        value_x = x_min + (x_max - x_min) * index / (samples - 1)
        try:
            value_y = float(function(value_x))
        except (ArithmeticError, TypeError, ValueError, OverflowError):
            continue
        if math.isfinite(value_y) and abs(value_y) <= 1_000_000:
            points.append(GraphPoint(x=round(value_x, 6), y=round(value_y, 6)))

    if len(points) < 2:
        return None
    return FormulaGraph(
        expression=expression_text,
        label=spec.label.strip() or "y = f(x)",
        x_min=x_min,
        x_max=x_max,
        points=points,
    )
