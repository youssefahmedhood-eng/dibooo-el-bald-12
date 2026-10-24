"""Radar chart metrics — 8 performance dimensions normalized 0-1."""

import numpy as np
from typing import Any


def compute_radar_metrics(sessions: list[dict[str, Any]]) -> dict:
    """
    Computes 8 radar dimensions from a list of sessions.

    Each session dict should contain:
      - hr_series: list[float]
      - speed_series: list[float]
      - accel_series: list[float]   (Y-axis peaks)
      - hr_max: float
      - resting_hr: float
      - duration_min: float
      - recovery_time_min: float    (time to drop 30 bpm post-session)
      - hrv_rmssd: float            (ms, if available)
      - week_index: int             (0=oldest, N=newest)

    Returns:
        {
            "metrics": {
                "aerobic_endurance": 0-1,
                "anaerobic_power": 0-1,
                "cardiac_recovery": 0-1,
                "pace_consistency": 0-1,
                "stride_efficiency": 0-1,
                "impact_load": 0-1,
                "hrv_score": 0-1,
                "weekly_progression": 0-1,
            }
        }
    """
    if not sessions:
        return {"metrics": {k: 0.0 for k in [
            "aerobic_endurance", "anaerobic_power", "cardiac_recovery",
            "pace_consistency", "stride_efficiency", "impact_load",
            "hrv_score", "weekly_progression",
        ]}}

    def safe_mean(lst): return float(np.mean(lst)) if lst else 0.0
    def safe_std(lst):  return float(np.std(lst))  if lst else 0.0

    # 1. Aerobic endurance: fraction of time in Z2-Z3
    aerobic_fracs = []
    for s in sessions:
        hr_s = s.get("hr_series", [])
        hr_max = s.get("hr_max", 190)
        rhr    = s.get("resting_hr", 55)
        if not hr_s: continue
        reserve = hr_max - rhr
        z2_lo = rhr + reserve * 0.60
        z3_hi = rhr + reserve * 0.80
        frac = sum(1 for h in hr_s if z2_lo <= h <= z3_hi) / len(hr_s)
        aerobic_fracs.append(frac)
    aerobic_endurance = safe_mean(aerobic_fracs)

    # 2. Anaerobic power: frequency and intensity of Z5 peaks
    anaerobic_scores = []
    for s in sessions:
        hr_s   = s.get("hr_series", [])
        hr_max = s.get("hr_max", 190)
        rhr    = s.get("resting_hr", 55)
        if not hr_s: continue
        reserve = hr_max - rhr
        z5_lo = rhr + reserve * 0.90
        z5_pct = sum(1 for h in hr_s if h >= z5_lo) / len(hr_s)
        anaerobic_scores.append(min(1.0, z5_pct * 10))  # 10% in Z5 = score 1.0
    anaerobic_power = safe_mean(anaerobic_scores)

    # 3. Cardiac recovery: inverse of recovery time (faster = better)
    recovery_times = [s.get("recovery_time_min", 10) for s in sessions if "recovery_time_min" in s]
    if recovery_times:
        norm_rec = [max(0, 1 - rt / 15) for rt in recovery_times]  # 0 min=1.0, 15+ min=0.0
        cardiac_recovery = safe_mean(norm_rec)
    else:
        cardiac_recovery = 0.5

    # 4. Pace consistency: inverse of speed variation coefficient
    consistency_scores = []
    for s in sessions:
        spd = s.get("speed_series", [])
        if not spd or safe_mean(spd) == 0: continue
        cv = safe_std(spd) / safe_mean(spd)
        consistency_scores.append(max(0, 1 - cv * 3))  # CV of 33% = 0
    pace_consistency = safe_mean(consistency_scores) if consistency_scores else 0.5

    # 5. Stride efficiency: distance per unit of accel impact
    efficiency_scores = []
    for s in sessions:
        spd  = s.get("speed_series", [])
        accel = s.get("accel_series", [])
        if not spd or not accel: continue
        mean_speed  = safe_mean(spd) / 3.6  # to m/s
        mean_impact = safe_mean([abs(a) for a in accel])
        if mean_impact > 0:
            efficiency = min(1.0, mean_speed / (mean_impact * 2))
            efficiency_scores.append(efficiency)
    stride_efficiency = safe_mean(efficiency_scores) if efficiency_scores else 0.5

    # 6. Impact load: cumulative accel (inverted — lower is better for joints)
    impact_loads = []
    for s in sessions:
        accel = s.get("accel_series", [])
        if not accel: continue
        mean_impact = safe_mean([abs(a) for a in accel])
        impact_loads.append(mean_impact)
    if impact_loads:
        norm_impact = [max(0, 1 - il / 4.0) for il in impact_loads]  # 4g = score 0
        impact_load = safe_mean(norm_impact)
    else:
        impact_load = 0.7

    # 7. HRV score: normalized RMSSD
    hrv_vals = [s.get("hrv_rmssd", 0) for s in sessions if s.get("hrv_rmssd", 0) > 0]
    if hrv_vals:
        mean_hrv = safe_mean(hrv_vals)
        hrv_score = min(1.0, mean_hrv / 80)  # 80ms RMSSD = elite (score 1.0)
    else:
        hrv_score = 0.5

    # 8. Weekly progression: linear trend of performance
    if len(sessions) >= 2:
        perf = []
        for s in sessions:
            hr_s = s.get("hr_series", [])
            hr_max = s.get("hr_max", 190)
            if hr_s:
                mean_hr = safe_mean(hr_s)
                perf.append(1 - mean_hr / hr_max)  # lower HR effort = better fitness
        if len(perf) >= 2:
            x = np.arange(len(perf))
            slope = np.polyfit(x, perf, 1)[0]
            weekly_progression = min(1.0, max(0.0, 0.5 + slope * 10))
        else:
            weekly_progression = 0.5
    else:
        weekly_progression = 0.5

    metrics = {
        "aerobic_endurance":    round(aerobic_endurance, 3),
        "anaerobic_power":      round(anaerobic_power, 3),
        "cardiac_recovery":     round(cardiac_recovery, 3),
        "pace_consistency":     round(pace_consistency, 3),
        "stride_efficiency":    round(stride_efficiency, 3),
        "impact_load":          round(impact_load, 3),
        "hrv_score":            round(hrv_score, 3),
        "weekly_progression":   round(weekly_progression, 3),
    }
    return {"metrics": metrics}
