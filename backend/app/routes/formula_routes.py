from __future__ import annotations

import logging

import sqlite3

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile
from starlette.concurrency import run_in_threadpool

from app.config import settings
from app.rate_limit import enforce_rate_limit
from app.schemas.formula_schema import FormulaAnalysisResponse
from app.security import get_optional_user
from app.services.formula_ocr_service import FormulaAnalysisUnavailable, analyze_formula_image
from app.services.image_processing import InvalidFormulaImage, normalize_formula_image


logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/analyze", response_model=FormulaAnalysisResponse)
async def analyze_formula(
    image: UploadFile = File(...),
    question: str | None = Form(default=None),
    current_user: sqlite3.Row | None = Depends(get_optional_user),
) -> FormulaAnalysisResponse:
    user_id = current_user["id"] if current_user else None
    enforce_rate_limit("formula", user_id, max_per_minute=10)
    declared_type = (image.content_type or "").lower()
    if declared_type and declared_type not in {"image/jpeg", "image/png", "image/webp"}:
        raise HTTPException(status_code=415, detail="Use uma imagem JPG, PNG ou WEBP.")

    content = await image.read(settings.formula_max_upload_bytes + 1)
    if len(content) > settings.formula_max_upload_bytes:
        raise HTTPException(
            status_code=413,
            detail=f"A imagem excede o limite de {settings.formula_max_upload_mb} MB.",
        )

    logger.info(
        "Formula upload received filename=%s declared_type=%s bytes=%d",
        image.filename,
        declared_type or "unknown",
        len(content),
    )
    try:
        normalized = await run_in_threadpool(
            normalize_formula_image,
            content,
            settings.formula_max_dimension,
        )
        return await run_in_threadpool(analyze_formula_image, normalized, question)
    except InvalidFormulaImage as error:
        raise HTTPException(status_code=422, detail=str(error)) from error
    except FormulaAnalysisUnavailable as error:
        raise HTTPException(status_code=503, detail=str(error)) from error
