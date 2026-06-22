from app.services.fuzzy_learning_service import FuzzyInputs, infer_fuzzy_level


def test_fuzzy_engine_changes_continuously_for_nearby_inputs() -> None:
    low = infer_fuzzy_level(FuzzyInputs(25, 20, 15, 10, 20))
    nearby = infer_fuzzy_level(FuzzyInputs(30, 25, 20, 15, 25))

    assert low.level in {"Muito Fácil", "Fácil"}
    assert nearby.score >= low.score
    assert nearby.score - low.score < 20


def test_fuzzy_engine_reaches_very_advanced_for_consistent_mastery() -> None:
    result = infer_fuzzy_level(FuzzyInputs(96, 94, 90, 92, 95))

    assert result.level == "Muito Avançado"
    assert result.score >= 80
