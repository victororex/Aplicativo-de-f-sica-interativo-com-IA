from datetime import datetime
from typing import Any

from pydantic import BaseModel, Field


class ImprovementStatsResponse(BaseModel):
    accuracy_rate: int
    study_quality: str
    studied_seconds: int
    questions_asked: int
    completed_phases: int
    total_phases: int


class AnalyticsEventRequest(BaseModel):
    event_type: str = Field(min_length=2, max_length=80)
    topic: str = Field(default="Fisica", min_length=2, max_length=120)
    is_correct: bool | None = None
    difficulty: str | None = Field(default=None, max_length=40)
    response_time_seconds: int = Field(default=0, ge=0, le=14_400)
    time_spent_seconds: int = Field(default=0, ge=0, le=14_400)
    metadata: dict[str, Any] = Field(default_factory=dict)


class StudySessionRequest(BaseModel):
    topic: str = Field(default="Fisica", min_length=2, max_length=120)


class StudySessionResponse(BaseModel):
    id: int
    topic: str
    started_at: datetime
    completed_at: datetime | None = None
    duration_seconds: int
    is_completed: bool


class EvolutionPointResponse(BaseModel):
    label: str
    accuracy_rate: int
    study_seconds: int
    questions: int


class TopicPerformanceResponse(BaseModel):
    topic: str
    attempts: int
    correct_answers: int
    accuracy_rate: int
    average_response_time_seconds: int
    mastery_score: int
    needs_review: bool


class PerformanceHistoryResponse(BaseModel):
    id: str
    topic: str
    activity: str
    result: str
    difficulty: str
    response_time_seconds: int
    timestamp: datetime


class RecommendationResponse(BaseModel):
    title: str
    reason: str
    topic: str
    action: str
    priority: int


class AdaptiveProfileResponse(BaseModel):
    explanation_level: str
    exercise_difficulty: str
    trend: str
    next_topic: str
    review_topics: list[str]
    suggested_questions: list[str]


class LearningDashboardResponse(BaseModel):
    total_study_seconds: int
    questions_answered: int
    questions_asked: int
    completed_sessions: int
    completed_lessons: int
    total_lessons: int
    completed_phases: int
    total_phases: int
    topics_studied: list[str]
    difficult_topics: list[TopicPerformanceResponse]
    topic_performance: list[TopicPerformanceResponse]
    daily_evolution: list[EvolutionPointResponse]
    weekly_evolution: list[EvolutionPointResponse]
    monthly_evolution: list[EvolutionPointResponse]
    accuracy_rate: int
    average_response_time_seconds: int
    performance_history: list[PerformanceHistoryResponse]
    recommendations: list[RecommendationResponse]
    adaptive_profile: AdaptiveProfileResponse
