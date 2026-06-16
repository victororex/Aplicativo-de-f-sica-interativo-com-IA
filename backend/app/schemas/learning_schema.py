from pydantic import BaseModel


class DailyQuestionResponse(BaseModel):
    id: str
    question: str
    options: list[str]
    correct_index: int
    explanation: str
    subject_id: str
    subject_name: str
    difficulty: str = "Media"
    topic: str = "Fisica"


class QuizSubmission(BaseModel):
    score: int
    total: int


class CampaignSubmission(BaseModel):
    score: int
    total: int


class QuizResultResponse(BaseModel):
    score: int
    total: int
    accuracy_rate: int


class DailyChallengeStatusResponse(BaseModel):
    completed_today: bool
    score: int | None = None
    total: int | None = None
    accuracy_rate: int | None = None
    completed_at: str | None = None


class CampaignExerciseResponse(BaseModel):
    id: str
    question: str
    options: list[str]
    correct_index: int
    explanation: str
    visual_type: str


class CampaignNodeResponse(BaseModel):
    id: str
    title: str
    description: str
    subject_id: str
    subject_name: str
    progress: float
    is_unlocked: bool
    stage_label: str
    visual_type: str
    exercises: list[CampaignExerciseResponse]


class AvatarItemResponse(BaseModel):
    id: str
    category: str
    name: str
