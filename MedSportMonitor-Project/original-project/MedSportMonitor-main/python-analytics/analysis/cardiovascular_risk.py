"""Cardiovascular risk index — composite clinical risk score."""

import numpy as np
from typing import Any


def compute_cardiovascular_risk(biometrics: dict, sessions: list[dict[str, Any]]) -> dict:
    """
    Returns a 0-100 risk index with detected clinical patterns.

    Biometrics keys:
      - resting_hr: float (bpm)
      - hr_max: float (bpm)
      - spo2_baseline: float (%)
      - age: int
      - sex: str ('M'/'F')

    Session keys (recent 7 days):
      - avg_hr: float
      - trimp: float
      - recovery_time_min: float
      - min_spo2: float
      - hrv_rmssd: float
      - hours_since_session: float
    """
    risk_factors = []
    patterns = []

    resting_hr   = biometrics.get("resting_hr", 60)
    hr_max       = biometrics.get("hr_max", 190)
    spo2_baseline = biometrics.get("spo2_baseline", 98.0)
    age          = biometrics.get("age", 25)

    # --- Resting HR component ---
    # >70 bpm at rest is a risk factor
    if resting_hr > 70:
        risk_factors.append(min(25, (resting_hr - 70) * 2.5))
    else:
        risk_factors.append(0)

    # --- SpO2 component ---
    if spo2_baseline < 95:
        risk_factors.append(min(30, (95 - spo2_baseline) * 8))
        patterns.append({
            "type": "LOW_BASELINE_SPO2",
            "description": f"Resting SpO2 is {spo2_baseline}% — below normal range.",
            "recommendation": "Pulmonary function evaluation recommended.",
            "severity": "WARNING",
        })
    else:
        risk_factors.append(0)

    # --- Session-derived components ---
    if sessions:
        recovery_times = [s.get("recovery_time_min", 5) for s in sessions]
        hrv_vals       = [s.get("hrv_rmssd", 40) for s in sessions]
        spo2_mins      = [s.get("min_spo2", 96) for s in sessions]
        loads          = [s.get("trimp", 50) for s in sessions]

        # Slow cardiac recovery (>8 min to drop 30 bpm)
        mean_rec = float(np.mean(recovery_times))
        if mean_rec > 8:
            factor = min(20, (mean_rec - 8) * 3)
            risk_factors.append(factor)
            patterns.append({
                "type": "SLOW_CARDIAC_RECOVERY",
                "description": f"Average cardiac recovery time is {mean_rec:.1f} min (threshold: 8 min).",
                "recommendation": "Consider reduced intensity. Cardiology consult if persists >3 sessions.",
                "severity": "ATTENTION",
            })
        else:
            risk_factors.append(0)

        # Low HRV (poor autonomic balance)
        mean_hrv = float(np.mean(hrv_vals))
        if mean_hrv < 25:
            factor = min(20, (25 - mean_hrv) * 1.5)
            risk_factors.append(factor)
            patterns.append({
                "type": "LOW_HRV",
                "description": f"Mean RMSSD is {mean_hrv:.1f} ms — indicates autonomic fatigue.",
                "recommendation": "Increase recovery priority. Check sleep quality and hydration.",
                "severity": "WARNING" if mean_hrv < 15 else "ATTENTION",
            })
        else:
            risk_factors.append(0)

        # SpO2 drop during exercise
        min_spo2_session = float(np.min(spo2_mins))
        if min_spo2_session < 90:
            risk_factors.append(25)
            patterns.append({
                "type": "EXERCISE_HYPOXIA",
                "description": f"SpO2 dropped to {min_spo2_session}% during exercise.",
                "recommendation": "Immediate medical evaluation required. Do not resume high-intensity training.",
                "severity": "CRITICAL",
            })
        elif min_spo2_session < 93:
            risk_factors.append(10)
            patterns.append({
                "type": "EXERCISE_DESATURATION",
                "description": f"SpO2 dropped to {min_spo2_session}% at peak effort.",
                "recommendation": "Monitor in subsequent sessions. Consider respiratory evaluation.",
                "severity": "WARNING",
            })
        else:
            risk_factors.append(0)

        # Overtraining signal
        total_load = float(np.sum(loads))
        if total_load > 600:
            risk_factors.append(min(15, (total_load - 600) / 40))
            patterns.append({
                "type": "HIGH_TRAINING_LOAD",
                "description": f"Weekly TRIMP load is {total_load:.0f} — above recommended threshold.",
                "recommendation": "Insert a recovery week before resuming high-load training.",
                "severity": "ATTENTION",
            })
        else:
            risk_factors.append(0)

    # --- Age adjustment ---
    if age > 40:
        risk_factors.append(min(10, (age - 40) * 0.5))
    else:
        risk_factors.append(0)

    # Final risk index
    raw_risk = float(np.sum(risk_factors))
    risk_index = min(100, int(raw_risk))

    if risk_index < 20:
        risk_level = "LOW"
    elif risk_index < 45:
        risk_level = "MODERATE"
    elif risk_index < 70:
        risk_level = "HIGH"
    else:
        risk_level = "VERY_HIGH"

    return {
        "risk_index": risk_index,
        "risk_level": risk_level,
        "patterns": patterns,
        "components": {k: v for k, v in zip(
            ["resting_hr", "spo2_baseline", "cardiac_recovery",
             "hrv", "min_spo2", "training_load", "age"],
            risk_factors
        )},
    }
