"""Team training load analysis — weekly heatmap and overtraining detection."""

import numpy as np
from typing import Any


DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
LOAD_THRESHOLDS = {"LOW": 50, "MODERATE": 150, "HIGH": 300, "MAXIMUM": 500}


def compute_team_load(athletes: list[dict[str, Any]], period_days: int = 7) -> dict:
    """
    Computes per-athlete daily load heatmap and overtraining risk flags.

    Each athlete dict:
      {
        "athlete_id": str,
        "name": str,
        "sessions": [{"day_index": 0-6, "trimp": float, "fatigue_index": float}]
      }

    Returns:
      {
        "heatmap": [{athlete, days: {0: trimp, ...}}],
        "overtraining_risks": [{athlete_id, name, fatigue_index}],
        "team_avg_load": float,
      }
    """
    heatmap = []
    overtraining = []
    all_loads = []

    for athlete in athletes:
        daily = {d: 0.0 for d in range(period_days)}
        total = 0.0
        for s in athlete.get("sessions", []):
            d = s.get("day_index", 0)
            t = s.get("trimp", 0.0)
            if 0 <= d < period_days:
                daily[d] += t
                total += t

        all_loads.append(total)
        heatmap.append({
            "athlete_id": athlete["athlete_id"],
            "name": athlete.get("name", "Unknown"),
            "days": daily,
            "weekly_total": round(total, 1),
        })

        fi = athlete.get("fatigue_index", 0)
        if fi > 75:
            overtraining.append({
                "athlete_id": athlete["athlete_id"],
                "name": athlete.get("name", "Unknown"),
                "fatigue_index": fi,
                "urgency": "HIGH" if fi > 85 else "MODERATE",
            })

    team_avg = round(float(np.mean(all_loads)), 1) if all_loads else 0.0

    return {
        "heatmap": heatmap,
        "overtraining_risks": overtraining,
        "team_avg_load": team_avg,
    }
