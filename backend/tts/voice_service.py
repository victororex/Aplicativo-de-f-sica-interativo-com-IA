from __future__ import annotations

import logging
import re
import threading
import time
from dataclasses import dataclass
from pathlib import Path
from uuid import uuid4

from app.config import settings
from tts.edge_pt import EdgeTTSClient
from tts.melotts import MeloTTSClient
from tts.openvoice import OpenVoiceV2Client


logger = logging.getLogger(__name__)


class VoiceCloneUnavailable(RuntimeError):
    """Raised when voice cloning cannot run in the current environment."""


@dataclass(frozen=True)
class VoiceCloneConfig:
    root_dir: Path = Path(__file__).resolve().parents[1]
    melotts_language: str = "ES"
    melotts_speaker: str | None = None
    melotts_speed: float = 1.0

    @property
    def checkpoints_dir(self) -> Path:
        return Path(getattr(settings, "openvoice_checkpoints_dir", self.root_dir / "checkpoints_v2"))

    @property
    def voices_dir(self) -> Path:
        return Path(getattr(settings, "voice_reference_dir", self.root_dir / "voices"))

    @property
    def tmp_dir(self) -> Path:
        return Path(getattr(settings, "voice_tmp_dir", self.root_dir / "tmp"))

    @property
    def professor_voice_path(self) -> Path:
        return self.voices_dir / "professor.wav"


class ProfessorVoiceService:
    """MeloTTS -> OpenVoice V2 pipeline cached for low-latency requests."""

    def __init__(self, config: VoiceCloneConfig | None = None) -> None:
        self.config = config or VoiceCloneConfig(
            melotts_language=getattr(settings, "melotts_language", "ES"),
            melotts_speaker=getattr(settings, "melotts_speaker", None),
            melotts_speed=float(getattr(settings, "melotts_speed", 1.0)),
        )
        self.engine = (getattr(settings, "tts_engine", "edge_pt") or "edge_pt").lower()
        self.device = self._detect_device()
        self.melo: MeloTTSClient | None = None
        self.openvoice: OpenVoiceV2Client | None = None
        self.source_se = None
        self.edge: EdgeTTSClient | None = None
        self._load_lock = threading.Lock()
        self._synthesis_lock = threading.Lock()
        self._loaded = False
        self._load_error: Exception | None = None

    def initialize(self) -> None:
        with self._load_lock:
            if self._loaded:
                return
            if self._load_error is not None:
                raise VoiceCloneUnavailable(str(self._load_error)) from self._load_error

            try:
                started = time.perf_counter()
                self.config.tmp_dir.mkdir(parents=True, exist_ok=True)
                self.config.voices_dir.mkdir(parents=True, exist_ok=True)

                if self.engine == "edge_pt":
                    self.edge = EdgeTTSClient(
                        voice=getattr(settings, "edge_tts_voice", "pt-BR-AntonioNeural"),
                        rate=getattr(settings, "edge_tts_rate", "+0%"),
                        pitch=getattr(settings, "edge_tts_pitch", "+0Hz"),
                    )
                    self._loaded = True
                    logger.info("Edge-TTS PT-BR pronto em %.2fs (voice=%s)",
                                time.perf_counter() - started, self.edge.voice)
                    return

                self.melo = MeloTTSClient(
                    language=self.config.melotts_language,
                    speaker=self.config.melotts_speaker,
                    speed=self.config.melotts_speed,
                    device=self.device,
                )
                self.melo.load()

                self.openvoice = OpenVoiceV2Client(
                    checkpoints_dir=self.config.checkpoints_dir,
                    reference_voice_path=self.config.professor_voice_path,
                    device=self.device,
                )
                self.openvoice.load()
                self.source_se = self.openvoice.load_source_embedding(self.melo.normalized_speaker_key)
                self._loaded = True
                logger.info("Voice clone pipeline ready in %.2fs", time.perf_counter() - started)
            except ModuleNotFoundError as error:
                self._load_error = error
                logger.warning("Voice clone pipeline unavailable: %s", error)
                raise VoiceCloneUnavailable(str(error)) from error
            except Exception as error:  # noqa: BLE001 - startup must not crash FastAPI.
                self._load_error = error
                logger.warning("Voice clone pipeline failed to initialize: %s", error)
                raise VoiceCloneUnavailable(str(error)) from error

    def synthesize(self, text: str) -> bytes:
        cleaned = prepare_text_for_voice(text)
        if not cleaned:
            raise ValueError("Texto vazio.")
        request_id = uuid4().hex
        logger.info("[VOICE %s] request received chars=%d", request_id, len(cleaned))
        if not self._loaded:
            logger.info("[VOICE %s] initializing pipeline (first request)", request_id)
            self.initialize()

        if self.engine == "edge_pt":
            if self.edge is None:
                raise VoiceCloneUnavailable("Edge-TTS PT-BR não inicializado.")
            started_total = time.perf_counter()
            result = self.edge.synthesize(cleaned[:4000])
            logger.info(
                "[VOICE %s] edge_pt finished total=%.2fs bytes=%d voice=%s",
                request_id, time.perf_counter() - started_total, len(result.audio), result.voice,
            )
            return result.audio

        if self.melo is None or self.openvoice is None or self.source_se is None:
            logger.error("[VOICE %s] pipeline state invalid: melo=%s openvoice=%s embedding=%s",
                         request_id, self.melo is not None, self.openvoice is not None, self.source_se is not None)
            raise VoiceCloneUnavailable("Pipeline de voz indisponível.")

        base_path = self.config.tmp_dir / f"{request_id}_melo.wav"
        cloned_path = self.config.tmp_dir / f"{request_id}_professor.wav"
        started_total = time.perf_counter()

        try:
            with self._synthesis_lock:
                logger.info("[VOICE %s] MeloTTS synthesizing", request_id)
                melo_result = self.melo.synthesize(cleaned[:4000], base_path)
                logger.info("[VOICE %s] MeloTTS done in %.2fs path=%s", request_id, melo_result.elapsed_seconds, melo_result.path)
                logger.info("[VOICE %s] OpenVoice converting to professor timbre", request_id)
                conversion_result = self.openvoice.convert(melo_result.path, self.source_se, cloned_path)
                logger.info("[VOICE %s] OpenVoice done in %.2fs path=%s", request_id, conversion_result.elapsed_seconds, conversion_result.path)

            audio = conversion_result.path.read_bytes()
            logger.info(
                "[VOICE %s] request finished total=%.2fs synthesis=%.2fs conversion=%.2fs bytes=%d",
                request_id,
                time.perf_counter() - started_total,
                melo_result.elapsed_seconds,
                conversion_result.elapsed_seconds,
                len(audio),
            )
            return audio
        except Exception as error:  # noqa: BLE001 - endpoint maps this to HTTP error.
            logger.exception("Voice clone request failed")
            raise VoiceCloneUnavailable("Falha ao gerar áudio com a voz clonada.") from error
        finally:
            for path in (base_path, cloned_path):
                try:
                    if path.exists():
                        path.unlink()
                except OSError:
                    logger.warning("Could not remove temporary audio file: %s", path)

    @staticmethod
    def _detect_device() -> str:
        try:
            import torch

            if torch.cuda.is_available():
                logger.info("CUDA available. Voice pipeline will use GPU.")
                return "cuda:0"
        except Exception:  # noqa: BLE001 - torch may not be installed during lightweight checks.
            logger.info("Torch unavailable while detecting device; defaulting to CPU.")
        logger.info("CUDA unavailable. Voice pipeline will use CPU.")
        return "cpu"

    def status(self) -> dict:
        """Return component-by-component health for /voice/status."""
        professor = self.config.professor_voice_path
        converter_dir = self.config.checkpoints_dir / "converter"
        converter_config = converter_dir / "config.json"
        converter_checkpoint = converter_dir / "checkpoint.pth"
        source_embedding = None
        if self.melo is not None and self.melo.speaker_key is not None:
            source_embedding = self.config.checkpoints_dir / "base_speakers" / "ses" / f"{self.melo.normalized_speaker_key}.pth"

        if self.engine == "edge_pt":
            return {
                "backend": True,
                "engine": "edge_pt",
                "voice": getattr(self.edge, "voice", None) if self.edge else None,
                "loaded": self._loaded,
                "load_error": str(self._load_error) if self._load_error else None,
                "audio_generation": self._loaded,
            }

        return {
            "backend": True,
            "engine": "melotts+openvoice",
            "professor_voice": professor.exists(),
            "professor_voice_path": str(professor),
            "checkpoints_dir": str(self.config.checkpoints_dir),
            "converter_config": converter_config.exists(),
            "converter_checkpoint": converter_checkpoint.exists(),
            "source_embedding": source_embedding.exists() if source_embedding is not None else None,
            "device": self.device,
            "loaded": self._loaded,
            "load_error": str(self._load_error) if self._load_error else None,
            "melotts": self.melo is not None,
            "openvoice": self.openvoice is not None,
            "converter": self.openvoice is not None,
            "embedding": self.source_se is not None,
            "audio_generation": self._loaded and self.source_se is not None,
        }


def prepare_text_for_voice(text: str) -> str:
    cleaned = re.sub(r"\s+", " ", text or "").strip()
    return (
        cleaned.replace("Dados do problema:", "Vamos pelos dados do problema.")
        .replace("Fórmula usada:", "A fórmula usada é:")
        .replace("Formula usada:", "A fórmula usada é:")
        .replace("Observação:", "Observação:")
        .replace("Observacao:", "Observação:")
        .replace("P = W / t", "Potência é igual ao trabalho dividido pelo tempo.")
        .replace("W = F · d", "Trabalho é igual à força vezes a distância.")
        .strip()
    )

voice_service = ProfessorVoiceService()
