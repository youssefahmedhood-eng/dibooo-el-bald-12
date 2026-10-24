"""
Cardiovascular model — realistic HR simulation with HRV and anomalies.

Based on:
  - Tanaka et al. (2001) HRmax = 208 - 0.7*age
  - First-order exponential response for warmup / recovery
  - Log-normal RR interval distribution for HRV
"""

import math
import random
import time
import numpy as np
from .athlete_profile import AthleteProfile


def _log_normal_noise(mean_hr: float, fitness: float) -> float:
    """Simulate HRV as noise on instantaneous HR using log-normal RR intervals."""
    mean_rr = 60_000.0 / mean_hr  # ms
    sdnn = 45 * (1 - fitness) + 15 * fitness  # SDNN in ms — fitter → more HRV paradox handled
    rr_noisy = np.random.lognormal(math.log(mean_rr), sdnn / mean_rr)
    return 60_000.0 / rr_noisy


def get_hr(profile: AthleteProfile) -> float:
    """
    Returns the instantaneous HR value for the athlete at the current moment.
    Applies anomaly modifiers on top of the physiological base.
    """
    base = profile.get_expected_hr_base()

    # Apply HRV
    hr = _log_normal_noise(base, profile.fitness)

    # Sprint bursts (random short-lived spikes during work phase)
    if profile.phase == "work":
        if random.random() < 0.003:  # ~0.3% chance per second of a sprint starting
            profile._sprint_until = time.time() + random.uniform(15, 35)
        sprint_active = getattr(profile, "_sprint_until", 0) > time.time()
        if sprint_active:
            sprint_t = max(0.0, getattr(profile, "_sprint_until", 0) - time.time())
            sprint_peak = random.uniform(15, 30) * profile.intensity
            rise_factor = math.exp(-sprint_t / 10)
            hr += sprint_peak * rise_factor

    # Anomaly overrides
    if profile.anomaly_collapse:
        collapse_t = time.time() - getattr(profile, "_collapse_start", time.time())
        hr = base - 40 * min(1.0, collapse_t / 10)
    elif profile.anomaly_tachycardia:
        hr = min(profile.hr_max * 1.1, hr + 35)
    elif profile.anomaly_bradycardia:
        hr = max(28, hr - 25)

    if profile.anomaly_irregularity:
        hr += random.gauss(0, 18)  # arrhythmia-like noise

    return round(max(profile.resting_hr * 0.85, min(profile.hr_max * 1.12, hr)), 1)
