"""Fatigue index calculation based on accumulated training load and recovery quality."""

import numpy as np
from typing import Any


def compute_fatigue_index(sessions: list[dict[str, Any]]) -> dict:
    """
    Computes a 0-100 fatigue index from recent sessions.

    Each session dict should contain:
      - trimp: float (Training Impulse)
      - duration_min: float
      - avg_hr: float
      - hr_max: float  (athlete's max)
      - hours_since_session: float

    Returns:
        {
            "index": 0-100,
            "level": "LOW" | "MODERATE" | "HIGH" | "CRITICAL",
            "recommendation": str,
            "acute_load": float,
            "chronic_load": float,
            "acr": float,           -- Acute:Chronic ratio
        }
    """
    if not sessions:
        return {
            "index": 0,
            "level": "LOW",
            "recommendation": "No recent sessions. Ready for training.",
            "acute_load": 0.0,
            "chronic_load": 0.0,
            "acr": 1.0,
        }

    # Weight sessions by recency (exponential decay, half-life ~4 days)
    acute_loads  = []
    chronic_loads = []

    for s in sessions:
        trimp    = s.get("trimp", 0.0)
        hours    = s.get("hours_since_session", 0.0)
        days_ago = hours / 24.0

        # Acute window: 7 days, weighted
        if days_ago <= 7:
            weight = np.exp(-days_ago * np.log(2) / 4)
            acute_loads.append(trimp * weight)

        # Chronic window: 28 days (but we only have 7 days of data in this endpoint)
        if days_ago <= 7:
            weight_chronic = np.exp(-days_ago * np.log(2) / 14)
            chronic_loads.append(trimp * weight_chronic)

    acute  = float(np.sum(acute_loads))
    chronic = float(np.sum(chronic_loads)) if chronic_loads else max(1.0, acute * 0.8)

    acr = acute / max(chronic, 1.0)

    # Map ACR to fatigue index
    # ACR < 0.8: undertraining    → low fatigue
    # ACR 0.8-1.3: optimal
    # ACR 1.3-1.5: moderate stress
    # ACR > 1.5: high injury risk
    if acr < 0.8:
        index = int(acr / 0.8 * 20)
    elif acr < 1.3:
        index = int(20 + (acr - 0.8) / 0.5 * 40)
    elif acr < 1.5:
        index = int(60 + (acr - 1.3) / 0.2 * 25)
    else:
        index = min(100, int(85 + (acr - 1.5) * 30))

    if index < 30:
        level = "LOW"
        rec = "Optimal condition for training. You can perform high-intensity sessions."
    elif index < 55:
        level = "MODERATE"
        rec = "Moderate fatigue. Include one recovery session before next high-intensity block."
    elif index < 75:
        level = "HIGH"
        rec = "High fatigue detected. Prioritize recovery: sleep, nutrition, and low-intensity movement."
    else:
        level = "CRITICAL"
        rec = "Overtraining risk. Rest for 48-72h minimum. Consult medical staff before resuming."

    return {
        "index": index,
        "level": level,
        "recommendation": rec,
        "acute_load": round(acute, 1),
        "chronic_load": round(chronic, 1),
        "acr": round(acr, 2),
    }
