"""SpO2 model — correlated with HR, with physiological lag and anomalies."""

import random
import time
from .athlete_profile import AthleteProfile

# SpO2 reacts 15-30 seconds after HR changes (stores recent HR buffer)
_hr_buffer: dict[str, list] = {}
SPO2_LAG_SAMPLES = 20  # ~20 seconds at 1 Hz


def get_spo2(profile: AthleteProfile, current_hr: float) -> float:
    """Returns instantaneous SpO2 (%) for the athlete."""
    aid = profile.athlete_id

    # Maintain rolling HR buffer for lag simulation
    buf = _hr_buffer.setdefault(aid, [profile.resting_hr] * SPO2_LAG_SAMPLES)
    buf.append(current_hr)
    if len(buf) > SPO2_LAG_SAMPLES:
        buf.pop(0)

    lagged_hr = sum(buf) / len(buf)  # use mean of buffer as lagged HR signal

    # Physiological desaturation model
    hr_effort_fraction = max(0.0, (lagged_hr - 0.85 * profile.hr_max) / (0.15 * profile.hr_max))
    spo2_base = 98.5 - 0.008 * lagged_hr * hr_effort_fraction * (1 - profile.fitness * 0.3)

    # Gaussian noise ±0.3%
    spo2 = spo2_base + random.gauss(0, 0.3)

    # Anomaly overrides
    if profile.anomaly_hypoxia:
        hypoxia_t = time.time() - getattr(profile, "_hypoxia_start", time.time())
        spo2 = max(86.0, spo2 - 8 * min(1.0, hypoxia_t / 20))
    elif profile.anomaly_collapse:
        spo2 = max(88.0, spo2 - 5)

    return round(max(85.0, min(100.0, spo2)), 1)
