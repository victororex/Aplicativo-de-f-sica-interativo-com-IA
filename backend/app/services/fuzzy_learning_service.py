from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class FuzzyInputs:
    accuracy: float
    response_speed: float
    study_frequency: float
    mission_progress: float
    lesson_progress: float


@dataclass(frozen=True)
class FuzzyResult:
    score: int
    level: str
    memberships: dict[str, float]


OUTPUT_SETS = {
    "Muito Fácil": (0.0, 10.0, 30.0),
    "Fácil": (10.0, 30.0, 50.0),
    "Intermediário": (30.0, 50.0, 70.0),
    "Avançado": (50.0, 70.0, 90.0),
    "Muito Avançado": (70.0, 90.0, 100.0),
}


def infer_fuzzy_level(inputs: FuzzyInputs) -> FuzzyResult:
    accuracy = _memberships(inputs.accuracy)
    speed = _memberships(inputs.response_speed)
    frequency = _memberships(inputs.study_frequency)
    missions = _memberships(inputs.mission_progress)
    lessons = _memberships(inputs.lesson_progress)

    strengths = {
        "Muito Fácil": max(
            min(accuracy["low"], speed["low"]),
            min(accuracy["low"], lessons["low"]),
            min(frequency["low"], missions["low"]),
        ),
        "Fácil": max(
            min(accuracy["low"], speed["medium"]),
            min(accuracy["medium"], frequency["low"]),
            min(missions["low"], lessons["medium"]),
        ),
        "Intermediário": max(
            min(accuracy["medium"], speed["medium"]),
            min(frequency["medium"], lessons["medium"]),
            min(accuracy["medium"], missions["medium"]),
        ),
        "Avançado": max(
            min(accuracy["high"], speed["medium"], frequency["medium"]),
            min(accuracy["medium"], speed["high"], lessons["high"]),
            min(accuracy["high"], missions["high"], lessons["medium"]),
        ),
        "Muito Avançado": max(
            min(accuracy["high"], speed["high"], frequency["high"]),
            min(accuracy["high"], missions["high"], lessons["high"]),
        ),
    }

    numerator = 0.0
    denominator = 0.0
    for x in range(101):
        aggregated = max(
            min(strengths[label], _triangular(x, *triangle))
            for label, triangle in OUTPUT_SETS.items()
        )
        numerator += x * aggregated
        denominator += aggregated
    score = round(numerator / denominator) if denominator else 50
    level = max(
        OUTPUT_SETS,
        key=lambda label: min(strengths[label], _triangular(score, *OUTPUT_SETS[label])),
    )
    return FuzzyResult(max(0, min(100, score)), level, strengths)


def response_speed_from_seconds(seconds: int) -> float:
    if seconds <= 0:
        return 50.0
    bounded = max(10, min(180, seconds))
    return max(0.0, min(100.0, 100.0 - (bounded - 10) * 100.0 / 170.0))


def _memberships(raw: float) -> dict[str, float]:
    value = max(0.0, min(100.0, raw))
    return {
        "low": _left_shoulder(value, 25.0, 55.0),
        "medium": _triangular(value, 25.0, 55.0, 80.0),
        "high": _right_shoulder(value, 55.0, 85.0),
    }


def _left_shoulder(x: float, full_until: float, zero_at: float) -> float:
    if x <= full_until:
        return 1.0
    if x >= zero_at:
        return 0.0
    return (zero_at - x) / (zero_at - full_until)


def _right_shoulder(x: float, zero_until: float, full_at: float) -> float:
    if x <= zero_until:
        return 0.0
    if x >= full_at:
        return 1.0
    return (x - zero_until) / (full_at - zero_until)


def _triangular(x: float, left: float, center: float, right: float) -> float:
    if x == center:
        return 1.0
    if x <= left or x >= right:
        return 0.0
    if x < center:
        return (x - left) / (center - left)
    return (right - x) / (right - center)
