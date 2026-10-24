"""
MedSport Monitor — Physiological Simulator
Publishes realistic multi-sensor data to MQTT for all configured athletes.

Usage:
    python simulator.py

Compatible with paho-mqtt 1.x and 2.x.
Can be run standalone — independent of the Java app.
Replace this module with ESP32 firmware for real hardware.
"""

import json
import logging
import signal
import sys
import threading
import time
from datetime import datetime, timezone

import paho.mqtt.client as mqtt

from config import (
    MQTT_BROKER, MQTT_PORT, MQTT_QOS, MQTT_KEEPALIVE,
    TEAM_ID, ATHLETE_PROFILES,
    HR_INTERVAL, SPO2_INTERVAL, GPS_INTERVAL, ACCEL_INTERVAL,
)
from physiological_models.athlete_profile import AthleteProfile
from physiological_models.heart_rate_model import get_hr
from physiological_models.spo2_model import get_spo2
from physiological_models.gps_model import get_gps
from physiological_models.accelerometer_model import get_accel

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
    datefmt="%H:%M:%S",
)
LOG = logging.getLogger("simulator")

_stop_event = threading.Event()

# Shared dict so Java coach panel can update intensity per athlete
_intensity_map: dict[str, float] = {}
_active_map:    dict[str, bool]  = {}


def iso_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"


def publish(client: mqtt.Client, athlete_id: str, sensor_type: str,
            value: float, unit: str, quality: float = 0.98) -> None:
    topic = f"medsport/{TEAM_ID}/{athlete_id}/{sensor_type}"
    payload = json.dumps({
        "timestamp": iso_now(),
        "athlete_id": athlete_id,
        "team_id": TEAM_ID,
        "value": round(value, 4),
        "unit": unit,
        "quality": quality,
    })
    try:
        result = client.publish(topic, payload, qos=MQTT_QOS)
        if hasattr(result, 'rc') and result.rc != 0:
            LOG.debug("Publish returned rc=%s for %s/%s", result.rc, athlete_id, sensor_type)
    except Exception as e:
        LOG.debug("Publish error for %s/%s: %s", athlete_id, sensor_type, e)


def athlete_thread(profile_config: dict, client: mqtt.Client) -> None:
    """Main simulation loop for one athlete — runs in its own thread."""
    profile = AthleteProfile(
        profile_config,
        intensity=_intensity_map.get(profile_config["athlete_id"], 0.75)
    )
    aid = profile.athlete_id
    LOG.info("Thread started: %s (%s)", profile_config.get("name", aid), aid)

    last_hr    = 0.0
    last_spo2  = 0.0
    last_gps   = 0.0
    last_accel = 0.0
    cached_hr  = profile.resting_hr
    cached_speed = 0.0

    while not _stop_event.is_set():
        # Respect active flag
        if not _active_map.get(aid, True):
            time.sleep(0.5)
            continue

        # Apply live intensity changes from coach panel
        profile.intensity = _intensity_map.get(aid, profile.intensity)

        now = time.monotonic()

        # HR — 1 Hz
        if now - last_hr >= HR_INTERVAL:
            cached_hr = get_hr(profile)
            publish(client, aid, "hr", cached_hr, "bpm")
            last_hr = now

        # SpO2 — 0.5 Hz (every 2 s)
        if now - last_spo2 >= SPO2_INTERVAL:
            spo2 = get_spo2(profile, cached_hr)
            publish(client, aid, "spo2", spo2, "%")
            last_spo2 = now

        # GPS — 1 Hz
        if now - last_gps >= GPS_INTERVAL:
            lat, lon, speed, alt = get_gps(profile)
            cached_speed = speed
            publish(client, aid, "gps_lat",   lat,   "deg")
            publish(client, aid, "gps_lon",   lon,   "deg")
            publish(client, aid, "gps_speed", speed, "km/h")
            publish(client, aid, "gps_alt",   alt,   "m")
            last_gps = now

        # Accelerometer — 10 Hz
        if now - last_accel >= ACCEL_INTERVAL:
            ax, ay, az, cadence = get_accel(profile, cached_speed)
            publish(client, aid, "accel_x",  ax,      "g")
            publish(client, aid, "accel_y",  ay,      "g")
            publish(client, aid, "accel_z",  az,      "g")
            publish(client, aid, "cadence",  cadence, "spm")
            last_accel = now

        time.sleep(0.05)


def _build_client() -> mqtt.Client:
    """Build a paho client compatible with both v1.x and v2.x."""
    try:
        # paho-mqtt 2.x
        client = mqtt.Client(
            callback_api_version=mqtt.CallbackAPIVersion.VERSION2,
            client_id=f"medsport-sim-{TEAM_ID}",
        )
    except AttributeError:
        # paho-mqtt 1.x fallback
        client = mqtt.Client(client_id=f"medsport-sim-{TEAM_ID}")
    return client


def _on_connect(client, userdata, flags, reason_code, properties=None):
    rc = reason_code if isinstance(reason_code, int) else reason_code.value
    if rc == 0:
        LOG.info("Connected to MQTT broker at %s:%d", MQTT_BROKER, MQTT_PORT)
    else:
        LOG.error("MQTT connection failed (rc=%d). Retrying...", rc)


def _on_disconnect(client, userdata, disconnect_flags, reason_code=None, properties=None):
    rc = reason_code if isinstance(reason_code, int) else (reason_code.value if reason_code else 0)
    if rc != 0:
        LOG.warning("Unexpected MQTT disconnect (rc=%d). Auto-reconnecting...", rc)


def main():
    LOG.info("MedSport Physiological Simulator starting")
    LOG.info("Team: %s | Athletes: %d", TEAM_ID, len(ATHLETE_PROFILES))

    # Initialise activity map — all athletes active by default
    for p in ATHLETE_PROFILES:
        _active_map[p["athlete_id"]] = True
        _intensity_map[p["athlete_id"]] = 0.75

    client = _build_client()
    client.on_connect    = _on_connect
    client.on_disconnect = _on_disconnect
    client.reconnect_delay_set(min_delay=1, max_delay=30)

    try:
        client.connect(MQTT_BROKER, MQTT_PORT, MQTT_KEEPALIVE)
    except Exception as e:
        LOG.error("Cannot connect to broker at %s:%d — %s", MQTT_BROKER, MQTT_PORT, e)
        LOG.error("Is Mosquitto running? Start it first: mosquitto -c mosquitto.conf")
        sys.exit(1)

    client.loop_start()
    time.sleep(0.5)

    # One thread per athlete
    threads = []
    for cfg in ATHLETE_PROFILES:
        t = threading.Thread(
            target=athlete_thread,
            args=(cfg, client),
            name=f"sim-{cfg['athlete_id']}",
            daemon=True,
        )
        t.start()
        threads.append(t)

    LOG.info("All %d athlete threads running. Press Ctrl+C to stop.", len(threads))

    def shutdown(sig, frame):
        LOG.info("Shutdown signal received")
        _stop_event.set()

    signal.signal(signal.SIGINT,  shutdown)
    signal.signal(signal.SIGTERM, shutdown)

    _stop_event.wait()

    LOG.info("Stopping threads...")
    for t in threads:
        t.join(timeout=2)
    client.loop_stop()
    client.disconnect()
    LOG.info("Simulator stopped cleanly")


if __name__ == "__main__":
    main()
