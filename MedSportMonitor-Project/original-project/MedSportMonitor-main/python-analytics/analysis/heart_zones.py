"""Heart rate zone analysis — Karvonen 5-zone model."""

from typing import Optional


ZONE_NAMES = {
    1: "Recovery",
    2: "Aerobic Base",
    3: "Aerobic",
    4: "Anaerobic Threshold",
    5: "Maximum",
}

ZONE_COLORS = {
    1: "#607D8B",
    2: "#4CAF50",
    3: "#FFC107",
    4: "#FF9800",
    5: "#F44336",
}


def get_zone_boundaries(hr_max: float, resting_hr: float = 0) -> dict[int, tuple[float, float]]:
    """Returns Karvonen zone boundaries (min, max) as % of HRmax or HRR."""
    reserve = hr_max - resting_hr if resting_hr > 0 else hr_max
    return {
        1: (resting_hr + reserve * 0.50, resting_hr + reserve * 0.60),
        2: (resting_hr + reserve * 0.60, resting_hr + reserve * 0.70),
        3: (resting_hr + reserve * 0.70, resting_hr + reserve * 0.80),
        4: (resting_hr + reserve * 0.80, resting_hr + reserve * 0.90),
        5: (resting_hr + reserve * 0.90, hr_max * 1.05),
    }


def classify_hr(hr: float, hr_max: float, resting_hr: float = 0) -> int:
    """Returns zone number (1-5) for a given HR value."""
    bounds = get_zone_boundaries(hr_max, resting_hr)
    for zone in range(5, 0, -1):
        lo, hi = bounds[zone]
        if hr >= lo:
            return zone
    return 1


def analyze_zones(hr_series: list[float], hr_max: float,
                  resting_hr: float = 0, sample_interval_s: float = 1.0) -> dict:
    """
    Computes time-in-zone distribution, predominant zone, and zone sequence.

    Returns:
        {
            "zones": {1: seconds, 2: ..., ...},
            "zones_pct": {1: %, ...},
            "predominant_zone": int,
            "total_seconds": int,
            "trimp": float,         -- Training Impulse score
        }
    """
    if not hr_series:
        return {"zones": {}, "zones_pct": {}, "predominant_zone": 0, "total_seconds": 0, "trimp": 0.0}

    counts = {z: 0 for z in range(1, 6)}
    trimp = 0.0

    for hr in hr_series:
        z = classify_hr(hr, hr_max, resting_hr)
        counts[z] += 1
        # TRIMP: each zone weighted exponentially
        trimp += sample_interval_s / 60 * z * 1.92 ** (z - 1)

    total = sum(counts.values())
    pct = {z: round(100 * v / total, 1) for z, v in counts.items()} if total > 0 else counts
    predominant = max(counts, key=counts.get)

    return {
        "zones": {z: int(v * sample_interval_s) for z, v in counts.items()},
        "zones_pct": pct,
        "predominant_zone": predominant,
        "total_seconds": int(total * sample_interval_s),
        "trimp": round(trimp, 1),
    }
