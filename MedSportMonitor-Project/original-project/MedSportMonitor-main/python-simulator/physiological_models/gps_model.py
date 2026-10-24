"""GPS model — simulates realistic routes based on sport type."""

import math
import random
import time
from .athlete_profile import AthleteProfile


# Base coordinates: Madrid (Athletics stadium)
BASE_LAT = 40.4139
BASE_LON = -3.7034

# Route definitions per sport
ROUTES = {
    "running": {
        # 400m athletics track (ellipse): a=65m, b=37m
        "type": "ellipse",
        "a_m": 65, "b_m": 37,
        "lap_m": 400,
    },
    "cycling": {
        # Out-and-back road route
        "type": "linear",
        "length_m": 5000,
        "heading_deg": 45,
    },
    "football": {
        # Random movement inside 105x68m pitch
        "type": "random_field",
        "w_m": 105, "h_m": 68,
    },
}

_METERS_PER_DEG_LAT = 111_320.0
_athlete_pos: dict[str, float] = {}  # athlete_id -> distance_m along route


def _meters_to_lat(m: float) -> float:
    return m / _METERS_PER_DEG_LAT


def _meters_to_lon(m: float, lat: float) -> float:
    return m / (_METERS_PER_DEG_LAT * math.cos(math.radians(lat)))


def get_gps(profile: AthleteProfile) -> tuple[float, float, float, float]:
    """
    Returns (lat, lon, speed_kmh, altitude_m) for the athlete.
    """
    aid = profile.athlete_id
    route_key = profile.sport if profile.sport in ROUTES else "running"
    route = ROUTES[route_key]

    # Speed based on phase and intensity
    max_speed_ms = 5.5 + profile.intensity * 4.0  # 5.5 m/s (20 km/h) at full intensity
    if profile.phase == "warmup":
        speed_ms = max_speed_ms * 0.55 * profile.get_phase_progress()
    elif profile.phase == "work":
        speed_ms = max_speed_ms * (0.75 + 0.25 * profile.intensity)
    elif profile.phase == "cooldown":
        speed_ms = max_speed_ms * (0.5 * (1 - profile.get_phase_progress()))
    else:
        speed_ms = 0.3

    speed_ms += random.gauss(0, 0.15)
    speed_ms = max(0.1, speed_ms)

    # Advance position
    dist_m = _athlete_pos.get(aid, 0.0)
    dist_m += speed_ms * 1.0  # 1 second step
    _athlete_pos[aid] = dist_m

    if route["type"] == "ellipse":
        lap_m = route["lap_m"]
        angle = (dist_m % lap_m) / lap_m * 2 * math.pi
        x_m = route["a_m"] * math.cos(angle)
        y_m = route["b_m"] * math.sin(angle)
        lat = BASE_LAT + _meters_to_lat(y_m) + random.gauss(0, 0.2 / 111_320)
        lon = BASE_LON + _meters_to_lon(x_m, BASE_LAT) + random.gauss(0, 0.2 / 111_320)

    elif route["type"] == "linear":
        length_m = route["length_m"]
        leg = int(dist_m / length_m) % 2
        pos = (dist_m % length_m) if leg == 0 else (length_m - dist_m % length_m)
        heading = math.radians(route["heading_deg"])
        x_m = pos * math.sin(heading)
        y_m = pos * math.cos(heading)
        lat = BASE_LAT + _meters_to_lat(y_m) + random.gauss(0, 0.3 / 111_320)
        lon = BASE_LON + _meters_to_lon(x_m, BASE_LAT) + random.gauss(0, 0.3 / 111_320)

    else:  # random_field
        # Brownian-like movement inside pitch
        prev_lat = getattr(profile, "_gps_lat", BASE_LAT)
        prev_lon = getattr(profile, "_gps_lon", BASE_LON)
        half_w = _meters_to_lon(route["w_m"] / 2, BASE_LAT)
        half_h = _meters_to_lat(route["h_m"] / 2)
        lat = max(BASE_LAT - half_h, min(BASE_LAT + half_h, prev_lat + random.gauss(0, 0.0001)))
        lon = max(BASE_LON - half_w, min(BASE_LON + half_w, prev_lon + random.gauss(0, 0.0001)))
        profile._gps_lat = lat
        profile._gps_lon = lon

    # Altitude: flat track with ±2m noise
    altitude = 650.0 + random.gauss(0, 2.0)

    speed_kmh = round(speed_ms * 3.6, 2)
    return round(lat, 7), round(lon, 7), speed_kmh, round(altitude, 1)
