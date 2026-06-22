from __future__ import annotations

import logging
import importlib
import sys
import time
import types
from dataclasses import dataclass
from pathlib import Path
from typing import Any


logger = logging.getLogger(__name__)


def _install_lazy_text_cleaner() -> None:
    """Prevent MeloTTS from loading tokenizers for every supported language."""
    module_name = "melo.text.cleaner"
    if module_name in sys.modules:
        return

    language_modules = {
        "ZH": "chinese",
        "JP": "japanese",
        "EN": "english",
        "ZH_MIX_EN": "chinese_mix",
        "KR": "korean",
        "FR": "french",
        "SP": "spanish",
        "ES": "spanish",
    }
    cleaner = types.ModuleType(module_name)

    def language_module(language: str):
        name = language_modules.get(language)
        if name is None:
            raise ValueError(f"Unsupported MeloTTS language: {language}")
        return importlib.import_module(f"melo.text.{name}")

    def clean_text(text: str, language: str):
        selected = language_module(language)
        normalized = selected.text_normalize(text)
        phones, tones, word2ph = selected.g2p(normalized)
        return normalized, phones, tones, word2ph

    def clean_text_bert(text: str, language: str, device=None):
        normalized, phones, tones, word2ph = clean_text(text, language)
        original_word2ph = list(word2ph)
        doubled_word2ph = [count * 2 for count in word2ph]
        if doubled_word2ph:
            doubled_word2ph[0] += 1
        bert = language_module(language).get_bert_feature(
            normalized,
            doubled_word2ph,
            device=device,
        )
        return normalized, phones, tones, original_word2ph, bert

    def text_to_sequence(text: str, language: str):
        from melo.text import cleaned_text_to_sequence

        normalized, phones, tones, _ = clean_text(text, language)
        del normalized
        return cleaned_text_to_sequence(phones, tones, language)

    cleaner.clean_text = clean_text
    cleaner.clean_text_bert = clean_text_bert
    cleaner.text_to_sequence = text_to_sequence
    sys.modules[module_name] = cleaner


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
        _install_lazy_text_cleaner()
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
