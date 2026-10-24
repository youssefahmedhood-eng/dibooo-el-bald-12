"""
Accelerometer model — 3-axis simulated gait cycle with realistic biomechanics.

Axes (ISO convention used in wearables):
  X: mediolateral (left-right)
  Y: vertical (up-down, dominant in running)
  Z: anteroposterior (forward-back)
"""

import math
import random
import time
from .athlete_profile import AthleteProfile


def get_accel(profile: AthleteProfile, speed_kmh: float) -> tuple[float, float, float, float]:
    """
    Returns (ax, ay, az, cadence_spm) — accelerations in g, cadence in steps/min.
    """
    speed_ms = speed_kmh / 3.6

    if speed_ms < 0.5:
        # Standing / resting — only gravity component
        noise = lambda s=0.01: random.gauss(0, s)
        return noise(), 1.0 + noise(), noise(), 0.0

    # Cadence: ~160-185 spm depending on speed and fitness
    cadence_spm = 160 + speed_ms * 2.5 + random.gauss(0, 3)
    cadence_spm = max(140, min(220, cadence_spm))
    step_freq_hz = cadence_spm / 60.0

    t = time.time()
    phase = (t * step_freq_hz * 2 * math.pi) % (2 * math.pi)

    # Y (vertical) — double-peak per step (heel strike + toe-off)
    impact_amplitude = 1.5 + speed_ms * 0.15  # higher speed → harder impact
    ay = 1.0 + impact_amplitude * abs(math.sin(phase)) + random.gauss(0, 0.08)

    # Z (forward) — propulsion / deceleration
    az = 0.3 * math.cos(phase) + random.gauss(0, 0.04)

    # X (lateral) — slight swing
    asymmetry = random.gauss(0, 0.05)  # subtle left-right asymmetry
    ax = 0.2 * math.sin(phase + math.pi / 4) + asymmetry + random.gauss(0, 0.03)

    # Anomaly: higher noise during irregularity
    if profile.anomaly_irregularity:
        ay += random.gauss(0, 0.4)

    return round(ax, 3), round(ay, 3), round(az, 3), round(cadence_spm, 1)
