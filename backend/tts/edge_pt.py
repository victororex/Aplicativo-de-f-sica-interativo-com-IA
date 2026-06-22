"""PT-BR TTS via Microsoft Edge Neural voices (edge-tts).

MeloTTS oficial não suporta português. Esta camada substitui o MeloTTS +
OpenVoice quando o app pede voz em PT-BR. Retorna MP3 — ExoPlayer detecta
o formato pelo conteúdo, então não precisa ser WAV.
"""
from __future__ import annotations

import asyncio
from concurrent.futures import ThreadPoolExecutor
import logging
import time
from dataclasses import dataclass

import edge_tts


logger = logging.getLogger(__name__)

DEFAULT_PT_VOICE = "pt-BR-AntonioNeural"   # masculina, calma — combina com "Renato"
DEFAULT_PT_RATE = "+0%"
DEFAULT_PT_PITCH = "+0Hz"


@dataclass(frozen=True)
class EdgeTTSResult:
    audio: bytes
    voice: str
    elapsed_seconds: float


class EdgeTTSClient:
    def __init__(
        self,
        voice: str = DEFAULT_PT_VOICE,
        rate: str = DEFAULT_PT_RATE,
        pitch: str = DEFAULT_PT_PITCH,
    ) -> None:
        self.voice = voice
        self.rate = rate
        self.pitch = pitch

    def synthesize(self, text: str) -> EdgeTTSResult:
        started = time.perf_counter()
        audio = self._run_render(text)
        elapsed = time.perf_counter() - started
        logger.info("edge-tts synth voice=%s bytes=%d in %.2fs", self.voice, len(audio), elapsed)
        return EdgeTTSResult(audio=audio, voice=self.voice, elapsed_seconds=elapsed)

    def _run_render(self, text: str) -> bytes:
        try:
            asyncio.get_running_loop()
        except RuntimeError:
            return asyncio.run(self._render(text))

        # FastAPI lifespan already owns this thread's event loop.
        with ThreadPoolExecutor(max_workers=1) as executor:
            return executor.submit(lambda: asyncio.run(self._render(text))).result()

    async def _render(self, text: str) -> bytes:
        communicate = edge_tts.Communicate(text, self.voice, rate=self.rate, pitch=self.pitch)
        chunks: list[bytes] = []
        async for chunk in communicate.stream():
            if chunk.get("type") == "audio":
                chunks.append(chunk["data"])
        if not chunks:
            raise RuntimeError("edge-tts retornou áudio vazio.")
        return b"".join(chunks)
