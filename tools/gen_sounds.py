#!/usr/bin/env python3
"""
Synthesizes the addon's sound effects (requires numpy + scipy + ffmpeg with libvorbis):

    python3 tools/gen_sounds.py

Writes mono 44.1 kHz .ogg files to src/main/resources/assets/rotp_bnrwk/sounds/.
Theme: the Stand is all about a ticking clock ("listen to my rhythm"), so every sound is built from
clock ticks, ratchets, springs and brass chimes. There are no samples from the manga / anime in here -
replace any file with your own recording of the same name and nothing else has to change.
"""
import os, subprocess, tempfile, wave
import numpy as np
from scipy import signal

SR = 44100
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "rotp_bnrwk", "sounds")
rng = np.random.default_rng(7)


def t_axis(sec):
    return np.arange(int(SR * sec)) / SR


def decay(t, tau):
    return np.exp(-t / tau)


def ring(freqs, amps, taus, sec):
    t = t_axis(sec)
    y = np.zeros_like(t)
    for f, a, tau in zip(freqs, amps, taus):
        y += a * np.sin(2 * np.pi * f * t) * decay(t, tau)
    return y


def noise(sec):
    return rng.standard_normal(int(SR * sec))


def bandpass(x, lo, hi):
    sos = signal.butter(2, [lo, hi], btype="band", fs=SR, output="sos")
    return signal.sosfilt(sos, x)


def highpass(x, f):
    return signal.sosfilt(signal.butter(2, f, btype="high", fs=SR, output="sos"), x)


def lowpass(x, f):
    return signal.sosfilt(signal.butter(2, f, btype="low", fs=SR, output="sos"), x)


def place(buf, clip, at_sec, gain=1.0):
    i = int(at_sec * SR)
    if i >= len(buf):
        return
    n = min(len(clip), len(buf) - i)
    buf[i:i + n] += clip[:n] * gain


def tick_clip(base=2400.0, body=900.0, sec=0.12):
    """One mechanical clock tick: a hard click, a short metallic ring and a woody body."""
    t = t_axis(sec)
    click = highpass(noise(sec), 3000) * decay(t, 0.0016)
    metal = ring([base, base * 2.31, base * 3.7], [0.8, 0.35, 0.15], [0.016, 0.010, 0.006], sec)
    wood = np.sin(2 * np.pi * body * t) * decay(t, 0.012) * 0.7
    return click * 0.9 + metal * 0.7 + wood


def normalize(x, peak=0.9):
    x = x - np.mean(x)
    m = np.max(np.abs(x))
    return x / m * peak if m > 0 else x


def fade(x, ms=6):
    n = int(SR * ms / 1000)
    x[:n] *= np.linspace(0, 1, n)
    x[-n:] *= np.linspace(1, 0, n)
    return x


def save(name, x):
    x = fade(normalize(x))
    pcm = (x * 32767).astype(np.int16)
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as d:
        wav = os.path.join(d, name + ".wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4",
                        os.path.join(OUT, name + ".ogg")], check=True)
    print("wrote", name + ".ogg", "%.2fs" % (len(x) / SR))


# ------------------------------------------------------------------------------------------ the sounds
def make_tick():
    save("tick", tick_clip(2600, 950, 0.12))


def make_tock():
    save("tock", tick_clip(1750, 620, 0.14))


def make_spring():
    """The pin snapping up: a ratchet click, a rising spring chirp and a bright metallic 'ting'."""
    sec = 0.7
    t = t_axis(sec)
    y = np.zeros_like(t)
    place(y, tick_clip(3000, 1100, 0.1), 0.0, 0.9)
    chirp_t = t_axis(0.09)
    chirp = np.sin(2 * np.pi * (500 * chirp_t + 6500 * chirp_t ** 2)) * decay(chirp_t, 0.05)
    place(y, chirp, 0.01, 0.6)
    place(y, ring([1320, 1320 * 2.76, 1320 * 5.4, 1320 * 8.9], [1, 0.5, 0.25, 0.1], [0.28, 0.12, 0.06, 0.03], sec), 0.07, 0.8)
    wob = 1 + 0.004 * np.sin(2 * np.pi * 38 * t) * decay(t, 0.3)  # spring wobble
    y *= wob
    save("pin_spring", y)


def make_plant():
    """Stand touches a thing: dull tap, sharp pin stab, two quick gear clicks."""
    sec = 0.5
    t = t_axis(sec)
    y = np.sin(2 * np.pi * 140 * t) * decay(t, 0.035) * 0.8
    y += lowpass(noise(sec), 700) * decay(t, 0.02) * 0.6
    place(y, tick_clip(3400, 1200, 0.1), 0.045, 1.0)
    place(y, tick_clip(2200, 800, 0.1), 0.15, 0.6)
    place(y, tick_clip(2800, 900, 0.1), 0.2, 0.5)
    place(y, ring([1900, 4300], [0.4, 0.2], [0.09, 0.04], 0.35), 0.06, 0.5)
    save("plant", y)


def make_throw():
    sec = 0.7
    t = t_axis(sec)
    env = np.sin(np.pi * np.clip(t / 0.45, 0, 1)) ** 2 * decay(t, 0.4)
    y = bandpass(noise(sec), 700, 6000) * env * 0.9
    for i in range(6):
        place(y, tick_clip(2200 + 250 * i, 850, 0.08), 0.02 + i * 0.045, 0.55)
    save("throw", y)


def make_smoke():
    sec = 1.6
    t = t_axis(sec)
    env = (1 - np.exp(-t / 0.05)) * decay(t, 0.55)
    y = highpass(noise(sec), 1200) * env * 0.8
    y += lowpass(noise(sec), 400) * env * 0.5
    for _ in range(22):
        place(y, tick_clip(rng.uniform(2800, 4200), 1000, 0.06), rng.uniform(0.05, 1.3), rng.uniform(0.15, 0.4))
    save("smoke", y)


def make_summon():
    """Clockwork winding up (accelerating ratchet), then a brass chime over a low swell."""
    sec = 2.2
    t = t_axis(sec)
    y = np.zeros_like(t)
    at, gap = 0.0, 0.14
    k = 0
    while at < 0.85:
        place(y, tick_clip(2000 + 120 * (k % 4), 800, 0.08), at, 0.55 + 0.04 * k)
        at += gap
        gap = max(gap * 0.86, 0.03)
        k += 1
    place(y, ring([660, 660 * 2.4, 660 * 3.9, 660 * 5.6], [1, 0.55, 0.3, 0.12], [0.9, 0.5, 0.3, 0.15], 1.5), 0.85, 0.75)
    place(y, ring([55, 110], [1, 0.4], [0.5, 0.3], 1.2), 0.82, 0.8)
    save("summon", y)


def make_barrage():
    """A fast burst of tick-tock hits - the Stand's 'rhythm' while it hammers away."""
    sec = 0.55
    y = np.zeros(int(SR * sec))
    for i in range(10):
        place(y, tick_clip(2600 if i % 2 == 0 else 1750, 950 if i % 2 == 0 else 620, 0.08), i * 0.05, 0.9)
    t = t_axis(sec)
    y += bandpass(noise(sec), 300, 2500) * decay(t, 0.3) * 0.12
    save("barrage", y)


def make_explode():
    """Boom with clock-parts shrapnel: low thump, sweeping noise blast and a tinkle of gears."""
    sec = 1.8
    t = t_axis(sec)
    y = np.sin(2 * np.pi * (48 + 40 * decay(t, 0.12)) * t) * decay(t, 0.28) * 1.0
    blast = noise(sec)
    # noise whose brightness falls over time (sweeping low-pass approximated by stacked bands)
    blast = lowpass(blast, 5000) * decay(t, 0.05) * 0.8 + lowpass(blast, 900) * decay(t, 0.35) * 0.9
    y += blast
    for _ in range(14):
        f = rng.uniform(2500, 6500)
        place(y, ring([f, f * 2.4], [0.5, 0.2], [0.05, 0.02], 0.25), rng.uniform(0.04, 0.7), rng.uniform(0.1, 0.3))
    place(y, ring([440, 440 * 2.76], [0.25, 0.1], [0.5, 0.2], 1.0), 0.05, 0.4)
    save("bomb_explode", y)


if __name__ == "__main__":
    for fn in (make_tick, make_tock, make_spring, make_plant, make_throw, make_smoke, make_summon, make_barrage, make_explode):
        fn()
