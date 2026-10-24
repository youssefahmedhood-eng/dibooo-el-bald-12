"""Simulator configuration — MQTT settings and athlete profiles."""

MQTT_BROKER   = "localhost"
MQTT_PORT     = 1883
MQTT_QOS      = 1
MQTT_KEEPALIVE = 60

TEAM_ID = "team_001"

# Publish interval in seconds
HR_INTERVAL     = 1.0
SPO2_INTERVAL   = 2.0
GPS_INTERVAL    = 1.0
ACCEL_INTERVAL  = 0.1   # 10 Hz accel

# Default session phases (seconds)
WARMUP_DURATION  = 300   # 5 min
WORK_DURATION    = 2400  # 40 min
COOLDOWN_DURATION = 600  # 10 min

ATHLETE_PROFILES = [
    {
        "athlete_id": "user_ath_001",
        "name": "Marco Torres",
        "age": 27,
        "sex": "M",
        "resting_hr": 52,
        "hr_max": 194,
        "weight_kg": 72.5,
        "height_cm": 178.0,
        "fitness_level": 0.85,
        "sport": "running",
    },
    {
        "athlete_id": "user_ath_002",
        "name": "Ana Serrano",
        "age": 25,
        "sex": "F",
        "resting_hr": 58,
        "hr_max": 188,
        "weight_kg": 58.0,
        "height_cm": 165.0,
        "fitness_level": 0.78,
        "sport": "running",
    },
    {
        "athlete_id": "user_ath_003",
        "name": "David Ruiz",
        "age": 28,
        "sex": "M",
        "resting_hr": 55,
        "hr_max": 191,
        "weight_kg": 78.0,
        "height_cm": 182.0,
        "fitness_level": 0.90,
        "sport": "running",
    },
    {
        "athlete_id": "user_ath_004",
        "name": "Lucia Morales",
        "age": 24,
        "sex": "F",
        "resting_hr": 60,
        "hr_max": 190,
        "weight_kg": 56.5,
        "height_cm": 168.0,
        "fitness_level": 0.70,
        "sport": "running",
    },
    {
        "athlete_id": "user_ath_005",
        "name": "Pablo Jimenez",
        "age": 26,
        "sex": "M",
        "resting_hr": 50,
        "hr_max": 195,
        "weight_kg": 70.0,
        "height_cm": 175.0,
        "fitness_level": 0.92,
        "sport": "running",
    },
    {
        "athlete_id": "user_ath_006",
        "name": "Elena Castro",
        "age": 23,
        "sex": "F",
        "resting_hr": 62,
        "hr_max": 186,
        "weight_kg": 54.0,
        "height_cm": 163.0,
        "fitness_level": 0.65,
        "sport": "running",
    },
]
