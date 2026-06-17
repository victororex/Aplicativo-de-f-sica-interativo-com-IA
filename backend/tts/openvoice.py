from __future__ import annotations

import logging
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any


logger = logging.getLogger(__name__)


@dataclass
class OpenVoiceConversionResult:
    path: Path
    elapsed_seconds: float


class OpenVoiceV2Client:
    """OpenVoice V2 tone converter with cached target speaker embedding."""

    def __init__(self, checkpoints_dir: Path, reference_voice_path: Path, device: str) -> None:
        self.checkpoints_dir = checkpoints_dir
        self.reference_voice_path = reference_voice_path
        self.device = device
        self.converter: Any | None = None
        self.target_se: Any | None = None
        self.torch: Any | None = None

    def load(self) -> None:
        started = time.perf_counter()
        converter_dir = self.checkpoints_dir / "converter"
        config_path = converter_dir / "config.json"
        checkpoint_path = converter_dir / "checkpoint.pth"
        if not config_path.exists() or not checkpoint_path.exists():
            raise RuntimeError(
                "OpenVoice V2 checkpoints not found. Run backend/scripts/download_openvoice_models.py."
            )
        if not self.reference_voice_path.exists():
            raise RuntimeError(f"Reference voice not found: {self.reference_voice_path}")

        logger.info("Loading OpenVoice V2 converter device=%s", self.device)
        import torch
        from openvoice.api import ToneColorConverter

        self.torch = torch
        self.converter = ToneColorConverter(str(config_path), device=self.device)
        self.converter.load_ckpt(str(checkpoint_path))

        logger.info("Extracting professor voice embedding from %s", self.reference_voice_path)
        # Direct extraction avoids OpenVoice's se_extractor import path, which pulls
        # faster-whisper -> av==10.*. That package often fails to build on Windows.
        self.target_se = self.converter.extract_se([str(self.reference_voice_path)])
        logger.info("OpenVoice V2 loaded in %.2fs", time.perf_counter() - started)

    def load_source_embedding(self, speaker_key: str) -> Any:
        if self.torch is None:
            raise RuntimeError("OpenVoice torch backend is not loaded.")
        source_path = self.checkpoints_dir / "base_speakers" / "ses" / f"{speaker_key}.pth"
        if not source_path.exists():
            raise RuntimeError(f"Source speaker embedding not found: {source_path}")
        return self.torch.load(str(source_path), map_location=self.device)

    def convert(self, source_audio: Path, source_se: Any, output_path: Path) -> OpenVoiceConversionResult:
        if self.converter is None or self.target_se is None:
            raise RuntimeError("OpenVoice V2 converter is not loaded.")

        started = time.perf_counter()
        output_path.parent.mkdir(parents=True, exist_ok=True)
        self.converter.convert(
            audio_src_path=str(source_audio),
            src_se=source_se,
            tgt_se=self.target_se,
            output_path=str(output_path),
            message="@FisicaInterativa",
        )
        elapsed = time.perf_counter() - started
        logger.info("OpenVoice conversion finished in %.2fs", elapsed)
        return OpenVoiceConversionResult(path=output_path, elapsed_seconds=elapsed)
