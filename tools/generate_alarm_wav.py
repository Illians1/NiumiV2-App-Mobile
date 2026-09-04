#!/usr/bin/env python3
"""Génère la sonnerie locale de Niumi (SPEC_ANDROID §10.2, étape 3 du plan MVP).

Aucun fichier audio tiers : le son est synthétisé, déterministe (aucun aléa, aucune
dépendance à l'horloge du poste), et versionné avec ce script plutôt qu'en binaire seul.
Régénérer le fichier avec ce script doit produire un WAV identique octet pour octet.

Format : mono, PCM 16 bits, 44,1 kHz, 6 secondes. Motif répété 6 fois : 750 ms de deux
sinusoïdes mélangées (740 Hz + 988 Hz) suivies de 250 ms de silence, avec un fondu de 10 ms
en entrée et en sortie de chaque segment sonore pour éviter tout clic.

Usage : python3 tools/generate_alarm_wav.py
"""

from __future__ import annotations

import math
import struct
import wave
from pathlib import Path

SAMPLE_RATE_HZ = 44_100
CHANNELS = 1
SAMPLE_WIDTH_BYTES = 2  # PCM 16 bits

TONE_FREQUENCIES_HZ = (740.0, 988.0)
TONE_DURATION_S = 0.750
SILENCE_DURATION_S = 0.250
PATTERN_COUNT = 6
FADE_DURATION_S = 0.010

PEAK_AMPLITUDE = 0.8  # fraction de l'amplitude maximale int16, marge contre l'écrêtage du mélange

OUTPUT_PATH = (
    Path(__file__).resolve().parent.parent
    / "androidApp"
    / "feature"
    / "ringing"
    / "src"
    / "main"
    / "res"
    / "raw"
    / "niumi_alarm.wav"
)


def _fade_gain(sample_index: int, segment_length: int) -> float:
    fade_samples = int(FADE_DURATION_S * SAMPLE_RATE_HZ)
    if fade_samples <= 0:
        return 1.0
    if sample_index < fade_samples:
        return sample_index / fade_samples
    remaining = segment_length - sample_index
    if remaining < fade_samples:
        return remaining / fade_samples
    return 1.0


def _tone_segment_samples() -> list[float]:
    length = int(TONE_DURATION_S * SAMPLE_RATE_HZ)
    samples = []
    for i in range(length):
        t = i / SAMPLE_RATE_HZ
        mixed = sum(math.sin(2 * math.pi * f * t) for f in TONE_FREQUENCIES_HZ)
        mixed /= len(TONE_FREQUENCIES_HZ)
        samples.append(mixed * _fade_gain(i, length))
    return samples


def _silence_segment_samples() -> list[float]:
    length = int(SILENCE_DURATION_S * SAMPLE_RATE_HZ)
    return [0.0] * length


def _generate_pcm_samples() -> list[float]:
    tone = _tone_segment_samples()
    silence = _silence_segment_samples()
    pattern = tone + silence
    return pattern * PATTERN_COUNT


def _to_int16_bytes(samples: list[float]) -> bytes:
    max_int16 = 32767
    packed = bytearray()
    for sample in samples:
        clamped = max(-1.0, min(1.0, sample * PEAK_AMPLITUDE))
        packed += struct.pack("<h", int(clamped * max_int16))
    return bytes(packed)


def main() -> None:
    samples = _generate_pcm_samples()
    pcm_bytes = _to_int16_bytes(samples)

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUTPUT_PATH), "wb") as wav_file:
        wav_file.setnchannels(CHANNELS)
        wav_file.setsampwidth(SAMPLE_WIDTH_BYTES)
        wav_file.setframerate(SAMPLE_RATE_HZ)
        wav_file.writeframes(pcm_bytes)

    duration_s = len(samples) / SAMPLE_RATE_HZ
    print(f"Écrit {OUTPUT_PATH} ({duration_s:.3f} s, {len(pcm_bytes)} octets PCM)")


if __name__ == "__main__":
    main()
