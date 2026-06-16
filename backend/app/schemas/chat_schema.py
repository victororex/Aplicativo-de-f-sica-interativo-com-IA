from typing import Optional

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    session_id: Optional[int] = None
    message: str = Field(..., min_length=1)
    subject: Optional[str] = None
    level: str = "universitario"
    context_title: Optional[str] = None
    context_topic: Optional[str] = None
    source: Optional[str] = None


class ChatResponse(BaseModel):
    session_id: Optional[int]
    user_message: str
    ai_response: str
    created_at: str


class MessageResponse(BaseModel):
    id: int
    sender: str
    content: str
    subject: Optional[str] = None
    level: Optional[str] = None
    created_at: str


class ChatSessionResponse(BaseModel):
    id: int
    title: str
    created_at: str
    updated_at: str


class ChatSessionDetailResponse(ChatSessionResponse):
    messages: list[MessageResponse]
