from pydantic import BaseModel, Field


class FormulaStep(BaseModel):
    title: str
    explanation: str
    latex: str | None = None


class GraphPoint(BaseModel):
    x: float
    y: float


class FormulaGraph(BaseModel):
    expression: str
    label: str
    x_min: float
    x_max: float
    points: list[GraphPoint]


class FormulaAnalysisResponse(BaseModel):
    ocr_text: str
    latex: str
    problem_statement: str
    steps: list[FormulaStep]
    final_answer: str
    graph: FormulaGraph | None = None
    narration_text: str
    warnings: list[str] = Field(default_factory=list)
