"""HRV analysis — RMSSD and basic time-domain metrics from RR intervals."""

import math
import numpy as np
from typing import Any


def compute_hrv(hr_series: list[float], sample_hz: float = 1.0) -> dict:
    """
    Computes time-domain HRV from a HR series sampled at sample_hz.
    For true HRV you need RR intervals at 1000 Hz; this is an approximation
    from a 1 Hz HR series suitable for wearable-grade data.
    """
    if len(hr_series) < 10:
        return {"rmssd": 0.0, "sdnn": 0.0, "mean_hr": 0.0, "pnn50": 0.0}

    rr_intervals = [60_000.0 / hr for hr in hr_series if hr > 0]

    if len(rr_intervals) < 2:
        return {"rmssd": 0.0, "sdnn": 0.0, "mean_hr": float(np.mean(hr_series)), "pnn50": 0.0}

    diffs = np.diff(rr_intervals)
    rmssd = float(np.sqrt(np.mean(diffs ** 2)))
    sdnn  = float(np.std(rr_intervals))
    pnn50 = float(np.sum(np.abs(diffs) > 50) / len(diffs) * 100)

    return {
        "rmssd": round(rmssd, 2),
        "sdnn":  round(sdnn, 2),
        "mean_hr": round(float(np.mean(hr_series)), 1),
        "pnn50": round(pnn50, 1),
    }


def compute_training_load(sessions: list[dict[str, Any]]) -> dict:
    """Computes ATL, CTL, TSB (Fitness-Fatigue model) from session TRIMPs."""
    if not sessions:
        return {"atl": 0.0, "ctl": 0.0, "tsb": 0.0}

    # Sort by recency (index 0 = oldest)
    sorted_s = sorted(sessions, key=lambda s: s.get("hours_since_session", 999), reverse=True)

    atl = 0.0  # Acute Training Load (7-day, k=1/7)
    ctl = 0.0  # Chronic Training Load (42-day, k=1/42)

    for s in sorted_s:
        trimp = s.get("trimp", 0.0)
        days  = s.get("hours_since_session", 0) / 24
        atl += trimp * math.exp(-days / 7)
        ctl += trimp * math.exp(-days / 42)

    return {
        "atl": round(atl, 1),
        "ctl": round(ctl, 1),
        "tsb": round(ctl - atl, 1),  # positive TSB = fresh; negative = fatigued
    }
