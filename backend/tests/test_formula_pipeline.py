from io import BytesIO

from fastapi.testclient import TestClient
from PIL import Image
import pytest

from app.config import settings
from app.services.formula_ocr_service import GraphSpec, _build_graph
from app.services.image_processing import InvalidFormulaImage, normalize_formula_image
from main import app


def make_image(width: int = 1200, height: int = 800) -> bytes:
    image = Image.new("RGB", (width, height), "white")
    output = BytesIO()
    image.save(output, format="PNG")
    return output.getvalue()


def test_normalize_formula_image_resizes_and_converts_to_jpeg() -> None:
    normalized = normalize_formula_image(make_image(3000, 1500), max_dimension=1000)

    assert normalized.media_type == "image/jpeg"
    assert normalized.width == 1000
    assert normalized.height == 500
    assert normalized.content.startswith(b"\xff\xd8")


def test_normalize_formula_image_rejects_invalid_content() -> None:
    with pytest.raises(InvalidFormulaImage):
        normalize_formula_image(b"not-an-image", max_dimension=1000)


def test_graph_builder_samples_supported_expression() -> None:
    graph = _build_graph(GraphSpec(expression="x**2", x_min=-2, x_max=2))

    assert graph is not None
    assert len(graph.points) == 81
    assert graph.points[0].y == 4
    assert graph.points[-1].y == 4


def test_graph_builder_rejects_unknown_symbols() -> None:
    assert _build_graph(GraphSpec(expression="x + secret", x_min=-2, x_max=2)) is None
    assert _build_graph(GraphSpec(expression="__import__(x)", x_min=-2, x_max=2)) is None


def test_formula_endpoint_returns_structured_mock_response() -> None:
    object.__setattr__(settings, "use_mock_ai", True)
    client = TestClient(app)

    response = client.post(
        "/formula/analyze",
        files={"image": ("formula.png", make_image(), "image/png")},
        data={"question": "Resolva e explique."},
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["content_type"] == "exercise"
    assert payload["visual_description"]
    assert payload["structured_data"]
    assert payload["latex"] == r"v = \frac{d}{t}"
    assert len(payload["steps"]) == 3
    assert len(payload["graph"]["points"]) == 81
    assert payload["narration_text"]


def test_formula_endpoint_rejects_non_image() -> None:
    client = TestClient(app)
    response = client.post(
        "/formula/analyze",
        files={"image": ("formula.txt", b"hello", "text/plain")},
    )

    assert response.status_code == 415
