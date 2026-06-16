from __future__ import annotations

from dataclasses import dataclass
from io import BytesIO
import logging

from PIL import Image, ImageOps, UnidentifiedImageError


logger = logging.getLogger(__name__)

ALLOWED_IMAGE_FORMATS = {
    "JPEG": "image/jpeg",
    "PNG": "image/png",
    "WEBP": "image/webp",
}
MAX_IMAGE_PIXELS = 40_000_000


class InvalidFormulaImage(ValueError):
    """Raised when an uploaded file is not a supported, decodable image."""


@dataclass(frozen=True)
class NormalizedImage:
    content: bytes
    media_type: str
    width: int
    height: int


def normalize_formula_image(content: bytes, max_dimension: int) -> NormalizedImage:
    if not content:
        raise InvalidFormulaImage("A imagem enviada esta vazia.")

    try:
        with Image.open(BytesIO(content)) as source:
            source.verify()
        with Image.open(BytesIO(content)) as source:
            image_format = source.format
            if image_format not in ALLOWED_IMAGE_FORMATS:
                raise InvalidFormulaImage("Use uma imagem JPG, PNG ou WEBP.")
            if source.width * source.height > MAX_IMAGE_PIXELS:
                raise InvalidFormulaImage("A imagem possui pixels demais para processamento seguro.")

            image = ImageOps.exif_transpose(source)
            if image.width < 64 or image.height < 64:
                raise InvalidFormulaImage("A imagem e pequena demais para reconhecer a formula.")

            image.thumbnail((max_dimension, max_dimension), Image.Resampling.LANCZOS)
            if image.mode not in {"RGB", "L"}:
                background = Image.new("RGB", image.size, "white")
                if "A" in image.getbands():
                    background.paste(image, mask=image.getchannel("A"))
                else:
                    background.paste(image)
                image = background
            elif image.mode == "L":
                image = image.convert("RGB")

            output = BytesIO()
            image.save(output, format="JPEG", quality=88, optimize=True)
            normalized = output.getvalue()
            logger.info(
                "Formula image normalized format=%s dimensions=%dx%d bytes=%d",
                image_format,
                image.width,
                image.height,
                len(normalized),
            )
            return NormalizedImage(
                content=normalized,
                media_type="image/jpeg",
                width=image.width,
                height=image.height,
            )
    except InvalidFormulaImage:
        raise
    except (Image.DecompressionBombError, UnidentifiedImageError, OSError, ValueError) as error:
        raise InvalidFormulaImage("O arquivo enviado nao e uma imagem valida.") from error
