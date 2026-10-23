"""Generates every sound in the mod from scratch: a small synthesizer, no recordings.

Writes mono .ogg files (positional sounds in Minecraft must be mono) to
src/main/resources/assets/skibidi/sounds, or to the folder given as the first argument.
Needs numpy and an ffmpeg with libvorbis. Each sound uses a fixed random seed, so running
it again gives the same files.
"""
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

SR = 44100
rng = np.random.default_rng(1)


# ---------------------------------------------------------------- building blocks

def seconds(d):
    return np.arange(int(SR * d)) / SR


def silence(d):
    return np.zeros(int(SR * d))


def noise(d):
    return rng.uniform(-1.0, 1.0, int(SR * d))


def phase_of(freq):
    """Running phase for a frequency given per sample."""
    return 2 * np.pi * np.cumsum(np.asarray(freq, dtype=float)) / SR


def saw(freq, d=None):
    f = np.full(int(SR * d), freq) if np.isscalar(freq) else freq
    p = np.cumsum(f) / SR
    return 2.0 * (p - np.floor(p + 0.5))


def square(freq, d=None):
    return np.sign(saw(freq, d))


def glide(f0, f1, d, curve=1.0):
    """A pitch sweep from f0 to f1 over d seconds; curve > 1 holds near f0 longer."""
    x = np.linspace(0.0, 1.0, int(SR * d)) ** curve
    return f0 * (f1 / f0) ** x


def decay(d, tau):
    return np.exp(-seconds(d) / tau)


def envelope(n, attack, release):
    """Linear fade in over `attack` seconds and out over `release` seconds."""
    env = np.ones(n)
    a, r = min(n, int(SR * attack)), min(n, int(SR * release))
    if a:
        env[:a] = np.linspace(0.0, 1.0, a)
    if r:
        env[n - r:] *= np.linspace(1.0, 0.0, r)
    return env


def biquad(x, b0, b1, b2, a1, a2):
    y = np.zeros_like(x)
    x1 = x2 = y1 = y2 = 0.0
    for i, xi in enumerate(x):
        yi = b0 * xi + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2, x1, y2, y1 = x1, xi, y1, yi
        y[i] = yi
    return y


def bandpass(x, freq, q):
    w = 2 * np.pi * freq / SR
    alpha = np.sin(w) / (2 * q)
    a0 = 1 + alpha
    return biquad(x, alpha / a0, 0.0, -alpha / a0, -2 * np.cos(w) / a0, (1 - alpha) / a0)


def lowpass(x, freq, q=0.707):
    w = 2 * np.pi * freq / SR
    alpha = np.sin(w) / (2 * q)
    a0 = 1 + alpha
    c = np.cos(w)
    return biquad(x, (1 - c) / 2 / a0, (1 - c) / a0, (1 - c) / 2 / a0, -2 * c / a0, (1 - alpha) / a0)


def highpass(x, freq, q=0.707):
    w = 2 * np.pi * freq / SR
    alpha = np.sin(w) / (2 * q)
    a0 = 1 + alpha
    c = np.cos(w)
    return biquad(x, (1 + c) / 2 / a0, -(1 + c) / a0, (1 + c) / 2 / a0, -2 * c / a0, (1 - alpha) / a0)


def sweep_filter(x, f0, f1, q, kind="band", block=256):
    """A filter whose cutoff glides from f0 to f1, updated every `block` samples."""
    out = np.zeros_like(x)
    blocks = range(0, len(x), block)
    freqs = f0 * (f1 / f0) ** np.linspace(0, 1, len(blocks))
    state = [0.0, 0.0, 0.0, 0.0]
    for f, start in zip(freqs, blocks):
        w = 2 * np.pi * f / SR
        alpha = np.sin(w) / (2 * q)
        a0 = 1 + alpha
        c = np.cos(w)
        if kind == "band":
            b = (alpha / a0, 0.0, -alpha / a0)
        else:
            b = ((1 - c) / 2 / a0, (1 - c) / a0, (1 - c) / 2 / a0)
        a1, a2 = -2 * c / a0, (1 - alpha) / a0
        x1, x2, y1, y2 = state
        seg = x[start:start + block]
        for i, xi in enumerate(seg):
            yi = b[0] * xi + b[1] * x1 + b[2] * x2 - a1 * y1 - a2 * y2
            x2, x1, y2, y1 = x1, xi, y1, yi
            out[start + i] = yi
        state = [x1, x2, y1, y2]
    return out


def drive(x, amount):
    """Soft clipping, for grit."""
    return np.tanh(x * amount) / np.tanh(amount)


def reverb(x, wet=0.25, size=1.0):
    """A small Schroeder reverb: four combs into two all-passes."""
    tail = int(SR * 0.6 * size)
    dry = np.concatenate([x, np.zeros(tail)])
    out = np.zeros_like(dry)
    for delay_ms, gain in ((29.7, 0.77), (37.1, 0.74), (41.1, 0.72), (43.7, 0.70)):
        d = int(SR * delay_ms / 1000 * size)
        y = dry.copy()
        for k in range(d, len(y), d):     # y[n] = x[n] + g * y[n - d], a block of d samples at a time
            end = min(k + d, len(y))
            y[k:end] += gain * y[k - d:end - d]
        out += y
    out /= 4
    for delay_ms, gain in ((5.0, 0.7), (1.7, 0.7)):
        d = int(SR * delay_ms / 1000)
        y = np.zeros_like(out)
        for n in range(len(out)):
            past = y[n - d] if n >= d else 0.0
            past_in = out[n - d] if n >= d else 0.0
            y[n] = -gain * out[n] + past_in + gain * past
        out = y
    return dry * (1 - wet) + out * wet


def mix(*parts):
    """Adds clips together, padding the shorter ones with silence. Each part is (clip, start_seconds)."""
    end = max(int(SR * start) + len(clip) for clip, start in parts)
    out = np.zeros(end)
    for clip, start in parts:
        s = int(SR * start)
        out[s:s + len(clip)] += clip
    return out


def seq(*clips):
    return np.concatenate(clips)


# ---------------------------------------------------------------- voices

# Formants (Hz, gain) for the vowels the toilets sing on.
VOWELS = {
    "i": ((270, 1.0), (2290, 0.35), (3010, 0.2)),
    "e": ((530, 1.0), (1840, 0.4), (2480, 0.2)),
    "a": ((730, 1.0), (1090, 0.6), (2440, 0.25)),
    "o": ((570, 1.0), (840, 0.6), (2410, 0.15)),
    "u": ((300, 1.0), (870, 0.4), (2240, 0.1)),
}


def vowel(f0, d, v, vibrato=0.0, bend=1.0):
    """A sung vowel: a buzzy source shaped by the vowel's three formants."""
    n = int(SR * d)
    t = np.arange(n) / SR
    pitch = f0 * bend ** (t / max(d, 1e-3)) * (1 + vibrato * np.sin(2 * np.pi * 5.5 * t) * np.clip(t / 0.12, 0, 1))
    source = saw(pitch) + 0.3 * rng.uniform(-1, 1, n) * 0.05
    out = np.zeros(n)
    for freq, gain in VOWELS[v]:
        out += gain * bandpass(source, freq, 6.0)
    return out * envelope(n, 0.015, 0.04)


def consonant(kind):
    """Short consonant noises: "sk", "b"/"p", "d"/"t", "k"."""
    if kind == "sk":
        return seq(highpass(noise(0.07), 3500) * envelope(int(SR * 0.07), 0.01, 0.02) * 0.5,
                   highpass(noise(0.015), 1500) * 0.6)
    if kind in ("b", "p"):
        return lowpass(noise(0.018), 900) * envelope(int(SR * 0.018), 0.001, 0.012) * (1.2 if kind == "b" else 0.9)
    if kind in ("d", "t"):
        return bandpass(noise(0.016), 2500, 1.5) * envelope(int(SR * 0.016), 0.001, 0.01) * 1.4
    if kind == "k":
        return bandpass(noise(0.025), 1800, 2.0) * envelope(int(SR * 0.025), 0.001, 0.015) * 1.5
    return silence(0.0)


def syllable(cons, v, f0, d, vibrato=0.01, bend=1.0):
    return seq(consonant(cons), vowel(f0, d, v, vibrato, bend))


def sing(seed, base):
    """A few bars of nonsense singing on a pentatonic scale: an original tune for every seed."""
    global rng
    rng = np.random.default_rng(seed)
    scale = [0, 2, 4, 7, 9, 12, 14]
    words = [[("sk", "i"), ("b", "i"), ("d", "i")], [("b", "o"), ("p", "a")], [("d", "a"), ("d", "a")],
             [("b", "i"), ("d", "u"), ("b", "a")], [("sk", "i"), ("b", "o")]]
    parts = []
    for _ in range(3):
        for c, v in words[rng.integers(len(words))]:
            step = scale[rng.integers(len(scale))]
            f0 = base * 2 ** (step / 12)
            held = rng.random() < 0.25
            parts.append(syllable(c, v, f0, 0.34 if held else 0.13, 0.025 if held else 0.01))
            parts.append(silence(0.03))
        parts.append(silence(0.08))
    return reverb(seq(*parts), wet=0.18)


def hurt(seed):
    global rng
    rng = np.random.default_rng(seed)
    return reverb(seq(consonant("k"), vowel(260, 0.22, "a", 0.0, 0.55)), wet=0.1)


def death(seed):
    """A gargling yell that sinks into bubbling water."""
    global rng
    rng = np.random.default_rng(seed)
    yell = vowel(240, 0.55, "o", 0.04, 0.4)
    gargle = yell * (0.6 + 0.4 * np.sin(2 * np.pi * 28 * seconds(0.55)))
    return mix((gargle, 0.0), (bubbles(0.9, 18), 0.35), (flush_noise(1.1) * 0.6, 0.3))


def bite():
    """A chomp: two hard clacks of porcelain and a short growl."""
    clack = bandpass(noise(0.03), 3200, 3.0) * decay(0.03, 0.006) * 2.5
    growl = drive(vowel(110, 0.18, "a", 0.0, 0.8) * 1.5, 3.0) * 0.5
    return mix((clack, 0.0), (clack * 0.8, 0.07), (growl, 0.02))


# ---------------------------------------------------------------- water

def bubbles(d, count, low=300, high=900):
    out = silence(d)
    for _ in range(count):
        start = rng.uniform(0, d - 0.06)
        length = rng.uniform(0.02, 0.05)
        f = rng.uniform(low, high)
        blip = np.sin(phase_of(glide(f, f * 1.8, length))) * decay(length, length / 3)
        s = int(SR * start)
        out[s:s + len(blip)] += blip * rng.uniform(0.3, 0.8)
    return out


def flush_noise(d):
    """Rushing water: noise through a band sweeping down, with a swirl wobble."""
    n = noise(d)
    body = sweep_filter(n, 2400, 500, 1.2)
    wobble = 0.65 + 0.35 * np.sin(2 * np.pi * np.linspace(9, 3, len(body)).cumsum() / SR)
    return body * wobble * envelope(len(body), 0.08, d * 0.5)


def flush(seed):
    global rng
    rng = np.random.default_rng(seed)
    click = bandpass(noise(0.02), 2000, 4.0) * decay(0.02, 0.004) * 1.5    # the handle
    return mix((click, 0.0), (flush_noise(1.3), 0.05), (bubbles(1.1, 22) * 0.35, 0.4))


def splash():
    return mix((sweep_filter(noise(0.45), 3000, 700, 0.9) * decay(0.45, 0.12), 0.0), (bubbles(0.4, 8) * 0.3, 0.05))


# ---------------------------------------------------------------- weapons

def laser(f_start, f_end, d, grit=1.5):
    """A zap: a falling square-ish tone with a little noise on top."""
    tone = square(glide(f_start, f_end, d, 0.6)) * 0.6 + saw(glide(f_start * 1.5, f_end * 1.5, d, 0.6)) * 0.4
    tone = lowpass(tone, 6000)
    hiss = highpass(noise(d), 4000) * 0.15
    return drive((tone + hiss) * decay(d, d / 2.5), grit)


def blaster():
    pew = laser(2200, 380, 0.28, 2.0)
    thump = np.sin(phase_of(glide(140, 50, 0.15))) * decay(0.15, 0.04) * 1.2
    return reverb(mix((pew, 0.0), (thump, 0.0)), wet=0.15)


def strider_bolt():
    whine = np.sin(phase_of(glide(600, 1800, 0.12))) * envelope(int(SR * 0.12), 0.05, 0.01) * 0.4
    shot = laser(1200, 160, 0.45, 2.5)
    boom = np.sin(phase_of(glide(90, 40, 0.3))) * decay(0.3, 0.08) * 1.3
    return reverb(seq(whine, mix((shot, 0.0), (boom, 0.0))), wet=0.2, size=1.3)


def giant_laser():
    charge = np.sin(phase_of(glide(200, 900, 0.25, 1.5))) * envelope(int(SR * 0.25), 0.2, 0.01) * 0.5
    blast = laser(700, 90, 0.8, 3.0)
    rumble = lowpass(noise(0.8), 200) * decay(0.8, 0.3) * 1.5
    return reverb(seq(charge, mix((blast, 0.0), (rumble, 0.0))), wet=0.25, size=1.6)


def spit():
    ptoo = mix((lowpass(noise(0.03), 800) * decay(0.03, 0.008) * 1.5, 0.0),
               (bandpass(noise(0.18), 1400, 1.0) * decay(0.18, 0.05), 0.02))
    return mix((ptoo, 0.0), (splash() * 0.6, 0.12))


# ---------------------------------------------------------------- big moves

def charge_rev():
    """An engine winding up."""
    d = 1.0
    f = glide(35, 120, d, 1.4)
    engine = drive(saw(f) + 0.6 * square(f * 0.5), 2.0) * envelope(int(SR * d), 0.05, 0.08)
    engine = lowpass(engine, 900)
    grind = bandpass(noise(d), 400, 1.0) * 0.4
    return (engine + grind) * 0.8


def crash():
    """Metal slamming into something: a deep hit, a clang, then rubble."""
    hit = np.sin(phase_of(glide(80, 30, 0.6))) * decay(0.6, 0.15) * 1.5
    clang = sum(np.sin(2 * np.pi * f * seconds(0.9)) * decay(0.9, 0.25) * g
                for f, g in ((311, 0.5), (523, 0.35), (877, 0.25), (1430, 0.15)))
    rubble = lowpass(noise(0.9), 1200) * decay(0.9, 0.25) * 0.8
    return reverb(mix((hit, 0.0), (clang, 0.0), (rubble, 0.02)), wet=0.25, size=1.4)


def slam():
    """A heavy landing: a boom with a burst of dirt."""
    boom = np.sin(phase_of(glide(110, 35, 0.7))) * decay(0.7, 0.18) * 1.6
    thud = lowpass(noise(0.25), 600) * decay(0.25, 0.05) * 1.2
    dirt = bandpass(noise(0.6), 2500, 0.8) * decay(0.6, 0.15) * 0.3
    return reverb(mix((boom, 0.0), (thud, 0.0), (dirt, 0.03)), wet=0.2, size=1.3)


def titan_step():
    boom = np.sin(phase_of(glide(70, 30, 0.5))) * decay(0.5, 0.12) * 1.5
    thud = lowpass(noise(0.2), 400) * decay(0.2, 0.04)
    return mix((boom, 0.0), (thud, 0.0))


def swoop():
    """Something huge rushing past."""
    d = 1.0
    wind = sweep_filter(noise(d), 300, 1800, 1.5) * np.sin(np.linspace(0, np.pi, int(SR * d))) ** 2
    hum = np.sin(phase_of(glide(90, 60, d))) * 0.3 * np.sin(np.linspace(0, np.pi, int(SR * d)))
    return wind * 1.4 + hum


def choir():
    """The Saint's hymn: an "ahh" chord over a soft bell."""
    chord = [196.0, 246.9, 293.7, 392.0]
    d = 2.0
    pad = sum(vowel(f, d, "a", 0.012) * 0.5 for f in chord)
    pad *= envelope(len(pad), 0.4, 0.8)
    bell = sum(np.sin(2 * np.pi * f * seconds(2.0)) * decay(2.0, 0.6) * g
               for f, g in ((784.0, 0.4), (1176.0, 0.2), (1568.0, 0.12)))
    return reverb(mix((pad, 0.0), (bell, 0.0)), wet=0.4, size=1.8)


def demonic_roar():
    d = 1.4
    f = glide(90, 45, d)
    growl = drive(vowel(90, d, "o", 0.06, 0.5) * 2.0 + saw(f) * 0.4, 4.0)
    growl *= envelope(len(growl), 0.05, 0.5)
    rumble = lowpass(noise(d), 180) * 1.2
    return reverb(growl * 0.8 + rumble * envelope(int(SR * d), 0.1, 0.6), wet=0.35, size=2.0)


# ---------------------------------------------------------------- DJ drum kit

def kick():
    return np.sin(phase_of(glide(150, 45, 0.3, 0.4))) * decay(0.3, 0.07) * 1.4


def snare():
    tone = np.sin(2 * np.pi * 190 * seconds(0.2)) * decay(0.2, 0.04)
    rattle = highpass(noise(0.2), 1800) * decay(0.2, 0.06)
    return (tone * 0.6 + rattle) * 1.1


def hat():
    return highpass(noise(0.06), 7000) * decay(0.06, 0.015) * 0.9


def clap():
    """Three quick hand slaps blurred together."""
    t = seconds(0.18)
    burst = bandpass(noise(0.18), 1500, 1.2)
    env = sum(np.where(t >= s, np.exp(-(t - s) / 0.01), 0.0) for s in (0.0, 0.012, 0.024)) + 0.5 * np.exp(-t / 0.05)
    return burst * env


# ---------------------------------------------------------------- the Alliance

def camera_flash():
    """Shutter click, then the flash charging back up."""
    click = bandpass(noise(0.015), 3000, 3.0) * decay(0.015, 0.003) * 2.0
    click2 = bandpass(noise(0.015), 2200, 3.0) * decay(0.015, 0.003) * 1.6
    whine = np.sin(phase_of(glide(2500, 6000, 0.45))) * envelope(int(SR * 0.45), 0.02, 0.2) * 0.12
    return mix((click, 0.0), (click2, 0.05), (whine, 0.07))


def speaker_blast():
    """A bass drop and a wobbling wave of sound."""
    d = 0.6
    bass = np.sin(phase_of(glide(80, 40, d))) * decay(d, 0.2) * 1.5
    wob = saw(glide(110, 55, d)) * (0.5 + 0.5 * np.sin(2 * np.pi * 12 * seconds(d))) * decay(d, 0.25)
    return reverb(bass + lowpass(wob, 800) * 0.8, wet=0.2)


def tv_static():
    d = 0.5
    hiss = bandpass(noise(d), 3500, 0.7)
    hum = (square(np.full(int(SR * d), 60.0)) * 0.15 + np.sin(2 * np.pi * 15734 * seconds(d)) * 0.02)
    chop = (rng.random(int(SR * d) // 400 + 1).repeat(400)[:int(SR * d)] > 0.3) * 1.0
    return (hiss * chop + hum) * envelope(int(SR * d), 0.01, 0.1)


# ---------------------------------------------------------------- story and menus

def tone(freq, d, kind="sq"):
    """A bright chiptune note."""
    wave_ = square(np.full(int(SR * d), freq)) * 0.5 + saw(np.full(int(SR * d), freq * 2)) * 0.2 if kind == "sq" \
        else np.sin(2 * np.pi * freq * seconds(d))
    return lowpass(wave_, 5000) * envelope(int(SR * d), 0.005, d * 0.6)


def notes(spec, kind="sq"):
    """[(frequency or 0 for a rest, seconds), ...]"""
    return seq(*(tone(f, d, kind) if f else silence(d) for f, d in spec))


C5, D5, E5, F5, G5, A5, B5, C6, E6, G6 = 523.3, 587.3, 659.3, 698.5, 784.0, 880.0, 987.8, 1046.5, 1318.5, 1568.0
G4, A4, B4, E4, D4, C4 = 392.0, 440.0, 493.9, 329.6, 293.7, 261.6


def mission_start():
    return reverb(notes([(G4, 0.08), (C5, 0.08), (E5, 0.08), (G5, 0.22)]), wet=0.25)


def objective():
    return reverb(notes([(E6, 0.08), (G6, 0.25)], "sine") * 0.8, wet=0.3)


def mission_complete():
    melody = notes([(C5, 0.1), (E5, 0.1), (G5, 0.1), (C6, 0.15), (0, 0.05), (G5, 0.1), (C6, 0.45)])
    bass = notes([(C4, 0.45), (196.0, 0.15), (C4, 0.55)], "sine") * 0.6
    return reverb(mix((melody, 0.0), (bass, 0.0)), wet=0.3)


def mission_failed():
    """A sad slide down."""
    parts = [tone(f, 0.28) * (0.9 - 0.1 * i) for i, f in enumerate((G4, 369.99, 349.2))]
    last = square(glide(329.6, 250, 0.7)) * 0.5 * envelope(int(SR * 0.7), 0.005, 0.4)
    return reverb(seq(*parts, lowpass(last, 4000)), wet=0.25)


def reward():
    """Coins and a sparkle."""
    coin = lambda: notes([(B5, 0.06), (E6, 0.2)], "sq") * 0.6
    sparkle = sum(np.sin(2 * np.pi * f * seconds(0.6)) * decay(0.6, 0.15) * 0.2 for f in (2093, 2637, 3136))
    return reverb(mix((coin(), 0.0), (coin(), 0.12), (sparkle, 0.2)), wet=0.3)


def click():
    return bandpass(noise(0.02), 2600, 2.5) * decay(0.02, 0.004) * 1.2 + \
        np.sin(2 * np.pi * 1200 * seconds(0.02)) * decay(0.02, 0.005) * 0.4


# ---------------------------------------------------------------- powers and tech

def chorus(f, d, spread=0.006, voices=3):
    """A few slightly detuned sines, for a shimmery energy tone. f is a pitch curve or a constant."""
    f = np.full(int(SR * d), f) if np.isscalar(f) else f
    return sum(np.sin(phase_of(f * (1.0 + spread * (i - (voices - 1) / 2)))) for i in range(voices)) / voices


def crackle(d, rate=60):
    """Random electric snaps."""
    n = int(SR * d)
    x = np.zeros(n)
    for start in rng.integers(0, max(1, n - 400), int(rate * d)):
        x[start:start + 300] += highpass(noise(300 / SR), 2500) * np.exp(-np.arange(300) / 40) * rng.uniform(0.3, 1.0)
    return x


def holoshield():
    """An energy field humming up to full strength, with a glassy shimmer on top."""
    d = 0.7
    hum = chorus(glide(180, 420, d, 0.5), d) * envelope(int(SR * d), 0.05, 0.3)
    shimmer = chorus(glide(1800, 2600, d), d, 0.01) * envelope(int(SR * d), 0.15, 0.4) * 0.25
    return reverb(hum + shimmer, wet=0.3)


def shield_hit():
    """Something glancing off the shield: a bright ping and a fizz."""
    d = 0.35
    ping = np.sin(phase_of(glide(2100, 1700, d))) * decay(d, 0.07)
    fizz = bandpass(noise(d), 5000, 1.0) * decay(d, 0.04) * 0.6
    return reverb(ping + fizz + crackle(d, 30) * 0.4, wet=0.25)


def record_scan():
    """A camera's record beep, then a scan sweeping out."""
    beep = notes([(B5 * 2, 0.07), (0, 0.04), (B5 * 2, 0.07)], "sine") * 0.6
    d = 0.8
    sweep = sweep_filter(saw(np.full(int(SR * d), 110.0)) * 0.6 + noise(d) * 0.3, 300, 4000, 6.0)
    sweep = sweep * envelope(int(SR * d), 0.05, 0.35)
    return reverb(seq(beep, sweep), wet=0.3)


def sonic_boots():
    """A bass kick from the boots and air rushing past."""
    d = 0.5
    thump = np.sin(phase_of(glide(120, 40, 0.25))) * decay(0.25, 0.06) * 1.4
    rush = sweep_filter(noise(d), 3000, 500, 1.5) * envelope(int(SR * d), 0.02, 0.3) * 0.8
    return mix((thump, 0.0), (rush, 0.0))


def anthem():
    """An original upbeat march for the Anthem Dance: 4 bars at 120 BPM (8 seconds), melody, bass and drums."""
    e, q, h = 0.25, 0.5, 1.0
    melody = notes([(G4, e), (C5, e), (E5, q), (D5, e), (C5, e), (D5, q),
                    (E5, e), (G5, e), (A5, q), (G5, h),
                    (A5, e), (G5, e), (E5, q), (F5, e), (E5, e), (D5, q),
                    (C5, e), (D5, e), (E5, e), (G5, e), (C6, h)]) * 0.55
    roots = (130.8, 110.0, 87.3, 98.0)                     # C, A minor, F, G
    bass = seq(*(lowpass(square(np.full(int(SR * e), r)) * 0.5, 900) * envelope(int(SR * e), 0.005, 0.12)
                 for r in roots for _ in range(8))) * 0.7
    beat = [(kick(), b * q) for b in range(16) if b % 2 == 0] + [(snare() * 0.6, b * q) for b in range(16) if b % 2 == 1] \
        + [(hat() * 0.5, i * e) for i in range(32)]
    total = 8.0
    music = mix((melody, 0.0), (bass, 0.0), *beat)[:int(SR * total)]
    # Cut to exactly 8 seconds (the reverb adds a tail), so it ends with the dance.
    return reverb(music, wet=0.15)[:int(SR * total)] * envelope(int(SR * total), 0.01, 0.15)


def hypnosis():
    """A woozy, wobbling tone that swirls up and back down."""
    d = 1.0
    t = seconds(d)
    pitch = 300 * (2.2 ** np.sin(np.pi * t / d)) * (1.0 + 0.04 * np.sin(2 * np.pi * 7 * t))
    tone = chorus(pitch, d, 0.012) * envelope(int(SR * d), 0.08, 0.35)
    return reverb(tone, wet=0.4, size=1.3)


def teleport():
    """Static rushing in, then a sudden pop out of existence."""
    d = 0.35
    rise = sweep_filter(noise(d), 600, 6000, 2.0) * envelope(int(SR * d), 0.3, 0.005) * 0.8
    pop = np.sin(phase_of(glide(500, 80, 0.15))) * decay(0.15, 0.03) * 1.2
    return reverb(seq(rise, pop), wet=0.3)


def burning_light():
    """A screen blazing to full brightness: an electric hum swelling, with fire crackling."""
    d = 0.9
    hum = lowpass(saw(np.full(int(SR * d), 120.0)) + saw(np.full(int(SR * d), 180.5)) * 0.6, 2500)
    hum = hum * envelope(int(SR * d), 0.15, 0.4) * 0.6
    roar = bandpass(noise(d), 900, 0.6) * envelope(int(SR * d), 0.1, 0.5) * 0.7
    return reverb(hum + roar + crackle(d, 50) * 0.5, wet=0.2)


def stun_flash():
    """A flash charging in a fraction of a second, then going off with an electric bang."""
    charge = np.sin(phase_of(glide(800, 5000, 0.18, 1.5))) * envelope(int(SR * 0.18), 0.15, 0.005) * 0.4
    d = 0.8
    bang = lowpass(noise(d), 1500) * decay(d, 0.12) * 1.2 + np.sin(phase_of(glide(160, 50, d))) * decay(d, 0.15)
    return reverb(seq(charge, bang + crackle(d, 40) * 0.6), wet=0.3, size=1.3)


def no_energy():
    """Two dull buzzes: not enough energy."""
    buzz = lowpass(square(np.full(int(SR * 0.09), 110.0)), 1200) * envelope(int(SR * 0.09), 0.005, 0.03) * 0.6
    return seq(buzz, silence(0.05), buzz)


def stun_rifle():
    """An electric bolt: a buzzing zap with snaps around it."""
    d = 0.4
    zap = laser(1400, 300, d, 2.5)
    buzz = square(np.full(int(SR * d), 60.0)) * decay(d, 0.12) * 0.25
    return reverb(zap + buzz + crackle(d, 60) * 0.6, wet=0.15)


def gravity_pull():
    """A deep "vwoom" sucking something in."""
    d = 0.6
    t = seconds(d)
    tone = np.sin(phase_of(glide(55, 220, d, 1.4))) * (0.6 + 0.4 * np.sin(2 * np.pi * 18 * t))
    air = sweep_filter(noise(d), 300, 2500, 2.0) * 0.3
    return reverb((tone + air) * envelope(int(SR * d), 0.1, 0.12), wet=0.3)


def energy_knife():
    """A humming blade cutting through: a buzz swept through a filter and a sharp slice."""
    d = 0.3
    hum = sweep_filter(saw(np.full(int(SR * d), 160.0)), 400, 3000, 3.0) * envelope(int(SR * d), 0.02, 0.15)
    slice_ = highpass(noise(0.08), 3000) * decay(0.08, 0.02) * 0.7
    return reverb(mix((hum, 0.0), (slice_, 0.05)), wet=0.15)


# ---------------------------------------------------------------- output

SOUNDS = {
    **{f"toilet/sing{i}": (lambda i=i: sing(100 + i, 165 + 12 * i)) for i in range(1, 5)},
    "toilet/hurt1": lambda: hurt(201),
    "toilet/hurt2": lambda: hurt(202),
    "toilet/death1": lambda: death(301),
    "toilet/death2": lambda: death(302),
    "toilet/bite": bite,
    "toilet/flush1": lambda: flush(401),
    "toilet/flush2": lambda: flush(402),
    "weapon/laser1": lambda: laser(1800, 300, 0.3),
    "weapon/laser2": lambda: laser(1600, 260, 0.33),
    "weapon/blaster": blaster,
    "weapon/strider_bolt": strider_bolt,
    "weapon/giant_laser": giant_laser,
    "weapon/spit": spit,
    "move/charge_rev": charge_rev,
    "move/crash": crash,
    "move/slam": slam,
    "move/titan_step": titan_step,
    "move/swoop": swoop,
    "move/choir": choir,
    "move/demonic_roar": demonic_roar,
    "dj/kick": kick,
    "dj/snare": snare,
    "dj/hat": hat,
    "dj/clap": clap,
    "alliance/camera_flash": camera_flash,
    "alliance/speaker_blast": speaker_blast,
    "alliance/tv_static": tv_static,
    "story/mission_start": mission_start,
    "story/objective": objective,
    "story/mission_complete": mission_complete,
    "story/mission_failed": mission_failed,
    "story/reward": reward,
    "ui/click": click,
    "power/holoshield": holoshield,
    "power/shield_hit": shield_hit,
    "power/record_scan": record_scan,
    "power/sonic_boots": sonic_boots,
    "power/anthem": anthem,
    "power/hypnosis": hypnosis,
    "power/teleport": teleport,
    "power/burning_light": burning_light,
    "power/stun_flash": stun_flash,
    "power/no_energy": no_energy,
    "weapon/stun_rifle": stun_rifle,
    "weapon/gravity_pull": gravity_pull,
    "weapon/energy_knife": energy_knife,
}


def write_ogg(path, x):
    peak = np.max(np.abs(x)) or 1.0
    x = x / peak * 0.89                     # about -1 dB
    x = np.concatenate([x, np.zeros(int(SR * 0.02))])
    pcm = (x * 32767).astype("<i2")
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".wav") as tmp:
        with wave.open(tmp.name, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp.name, "-c:a", "libvorbis", "-q:a", "4",
                        "-map_metadata", "-1", "-fflags", "+bitexact", str(path)], check=True)


out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent / \
    "src/main/resources/assets/skibidi/sounds"
only = set(sys.argv[2:])
for name, make in SOUNDS.items():
    if only and name not in only:
        continue
    rng = np.random.default_rng(sum(map(ord, name)))
    write_ogg(out / f"{name}.ogg", make())
    print("wrote", out / f"{name}.ogg")
