from pydantic import BaseModel


class SubjectResponse(BaseModel):
    id: str
    name: str
    description: str
    exam_focus: str
    total_lessons: int
    completed_lessons: int
    progress: float
    is_completed: bool


class LessonSummaryResponse(BaseModel):
    id: str
    subject_id: str
    subject_name: str
    title: str
    description: str
    exam_tags: str
    is_completed: bool


class LessonDetailResponse(LessonSummaryResponse):
    content: str


class ProgressSummaryResponse(BaseModel):
    user_id: int | None
    overall_completion: float
    completed_lessons: list[str]
    current_module: str
    subjects: list[SubjectResponse]
