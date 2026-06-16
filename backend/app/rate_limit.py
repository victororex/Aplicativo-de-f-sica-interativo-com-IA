"""In-memory rate limit por usuário/IP para rotas sensíveis.

Implementação intencionalmente simples (token bucket sliding window).
Não é distribuído — para multi-instância, trocar por Redis.
"""
from __future__ import annotations

import threading
import time
from collections import defaultdict, deque

from fastapi import HTTPException

_WINDOW_SECONDS = 60.0
_state: dict[tuple[str, str], deque[float]] = defaultdict(deque)
_lock = threading.Lock()


def enforce_rate_limit(bucket: str, user_id: int | None, *, max_per_minute: int) -> None:
    """Levanta HTTP 429 se o user (ou anon) exceder max_per_minute no bucket."""
    key = (bucket, str(user_id) if user_id is not None else "anon")
    now = time.monotonic()
    with _lock:
        events = _state[key]
        cutoff = now - _WINDOW_SECONDS
        while events and events[0] < cutoff:
            events.popleft()
        if len(events) >= max_per_minute:
            raise HTTPException(
                status_code=429,
                detail="Muitas requisições em pouco tempo. Aguarde alguns segundos e tente novamente.",
            )
        events.append(now)
