from pydantic import BaseModel


class ImprovementStatsResponse(BaseModel):
    accuracy_rate: int
    study_quality: str
    studied_seconds: int
    questions_asked: int
    completed_phases: int
    total_phases: int
