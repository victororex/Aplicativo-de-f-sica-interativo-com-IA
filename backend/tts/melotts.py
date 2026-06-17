from __future__ import annotations

import logging
import os
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any


logger = logging.getLogger(__name__)


def _prefer_local_model_cache() -> None:
    """Avoid slow network retries when MeloTTS/OpenVoice assets are already cached."""
    os.environ.setdefault("HF_HUB_OFFLINE", "1")
    os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")
    os.environ.setdefault("HF_DATASETS_OFFLINE", "1")


@dataclass
class MeloSynthesisResult:
    path: Path
    speaker_key: str
    elapsed_seconds: float


class MeloTTSClient:
    """Small wrapper around MeloTTS.

    The model is created once and reused for all requests. MeloTTS exposes
    speaker ids through hps.data.spk2id; OpenVoice needs the matching speaker
    key to load the source speaker embedding.
    """

    def __init__(self, language: str, speaker: str | None, speed: float, device: str) -> None:
        self.language = language
        self.requested_speaker = speaker
        self.speed = speed
        self.device = device
        self.model: Any | None = None
        self.speaker_ids: dict[str, int] = {}
        self.speaker_key: str | None = None
        self.speaker_id: int | None = None

    def load(self) -> None:
        started = time.perf_counter()
        logger.info("Loading MeloTTS model language=%s device=%s", self.language, self.device)
        _prefer_local_model_cache()
        import nltk

        nltk_data_dir = Path(__file__).resolve().parents[1] / "nltk_data"
        if str(nltk_data_dir) not in nltk.data.path:
            nltk.data.path.insert(0, str(nltk_data_dir))
        from melo.api import TTS

        self.model = TTS(language=self.language, device=self.device)
        self.speaker_ids = dict(self.model.hps.data.spk2id)
        if not self.speaker_ids:
            raise RuntimeError("MeloTTS did not expose any speaker id.")

        self.speaker_key = self._select_speaker_key()
        self.speaker_id = int(self.speaker_ids[self.speaker_key])
        logger.info(
            "MeloTTS loaded in %.2fs using speaker=%s",
            time.perf_counter() - started,
            self.speaker_key,
        )

    def synthesize(self, text: str, output_path: Path) -> MeloSynthesisResult:
        if self.model is None or self.speaker_key is None or self.speaker_id is None:
            raise RuntimeError("MeloTTS model is not loaded.")

        started = time.perf_counter()
        output_path.parent.mkdir(parents=True, exist_ok=True)
        self.model.tts_to_file(text, self.speaker_id, str(output_path), speed=self.speed)
        elapsed = time.perf_counter() - started
        logger.info("MeloTTS synthesis finished in %.2fs", elapsed)
        return MeloSynthesisResult(path=output_path, speaker_key=self.normalized_speaker_key, elapsed_seconds=elapsed)

    @property
    def normalized_speaker_key(self) -> str:
        if self.speaker_key is None:
            raise RuntimeError("MeloTTS speaker is not selected.")
        return self.speaker_key.lower().replace("_", "-")

    def _select_speaker_key(self) -> str:
        if self.requested_speaker:
            for key in self.speaker_ids:
                if key.lower() == self.requested_speaker.lower():
                    return key
            raise RuntimeError(
                f"MeloTTS speaker '{self.requested_speaker}' not found. "
                f"Available: {', '.join(self.speaker_ids.keys())}"
            )
        return next(iter(self.speaker_ids))
