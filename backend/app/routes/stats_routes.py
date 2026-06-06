from fastapi import APIRouter
import sqlite3

from fastapi import Depends

from app.database import get_db
from app.schemas.stats_schema import ImprovementStatsResponse
from app.security import get_optional_user
from app.services.stats_service import get_improvement_stats

router = APIRouter()


@router.get("/improvement", response_model=ImprovementStatsResponse)
def improvement_stats(
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row | None = Depends(get_optional_user),
):
    return get_improvement_stats(db=db, user_id=current_user["id"] if current_user else None)
