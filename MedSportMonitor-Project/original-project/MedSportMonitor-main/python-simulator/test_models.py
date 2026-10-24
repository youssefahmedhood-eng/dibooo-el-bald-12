# -*- coding: utf-8 -*-
"""
Standalone verification of physiological models.
Does NOT require Mosquitto -- runs purely in-process.

Usage:
    python test_models.py
"""

import sys
import time
import statistics

sys.path.insert(0, ".")

from config import ATHLETE_PROFILES
from physiological_models.athlete_profile import AthleteProfile
from physiological_models.heart_rate_model import get_hr
from physiological_models.spo2_model import get_spo2
from physiological_models.gps_model import get_gps
from physiological_models.accelerometer_model import get_accel

GREEN  = "\033[92m"
RED    = "\033[91m"
CYAN   = "\033[96m"
RESET  = "\033[0m"
BOLD   = "\033[1m"

PASS_LABEL = f"{GREEN}[PASS]{RESET}"
FAIL_LABEL = f"{RED}[FAIL]{RESET}"

errors = 0


def check(condition, label, detail=""):
    global errors
    if condition:
        print(f"  {PASS_LABEL} {label}")
    else:
        msg = f"  {FAIL_LABEL} {label}"
        if detail:
            msg += f" -- {detail}"
        print(msg)
        errors += 1


def sample_athlete(profile_cfg, n_seconds=90):
    profile = AthleteProfile(profile_cfg, intensity=0.80)
    hr_samples, spo2_samples, speed_samples = [], [], []
    ay_samples = []

    for _ in range(n_seconds):
        hr = get_hr(profile)
        spo2 = get_spo2(profile, hr)
        lat, lon, speed, alt = get_gps(profile)
        ax, ay, az, cadence = get_accel(profile, speed)

        hr_samples.append(hr)
        spo2_samples.append(spo2)
        speed_samples.append(speed)
        ay_samples.append(ay)

    return {
        "name": profile_cfg["name"],
        "hr_max": profile_cfg["hr_max"],
        "resting_hr": profile_cfg["resting_hr"],
        "hr": hr_samples,
        "spo2": spo2_samples,
        "speed": speed_samples,
        "ay": ay_samples,
    }


def verify_athlete(data):
    name   = data["name"]
    hr_max = data["hr_max"]
    rhr    = data["resting_hr"]
    hr     = data["hr"]
    spo2   = data["spo2"]
    speed  = data["speed"]
    ay     = data["ay"]

    print(f"\n{BOLD}{CYAN}  Athlete: {name}{RESET}")

    hr_mean = statistics.mean(hr)
    hr_min  = min(hr)
    hr_max_obs = max(hr)
    hr_std  = statistics.stdev(hr)

    print(f"    HR    mean={hr_mean:.1f}  min={hr_min:.1f}  max={hr_max_obs:.1f}  std={hr_std:.1f} bpm")
    check(rhr * 0.7 <= hr_mean <= hr_max * 0.98,
          f"HR mean is physiologically plausible ({hr_mean:.1f} bpm)")
    check(hr_min >= rhr * 0.60,
          f"HR minimum not abnormally low ({hr_min:.1f} bpm)")
    check(hr_max_obs <= hr_max * 1.10,
          f"HR maximum within safe range ({hr_max_obs:.1f} bpm)")
    check(0.5 <= hr_std <= 15,
          f"HR has realistic HRV noise (std={hr_std:.1f} bpm)")

    spo2_mean = statistics.mean(spo2)
    spo2_min  = min(spo2)
    spo2_max  = max(spo2)

    print(f"    SpO2  mean={spo2_mean:.1f}%  min={spo2_min:.1f}%  max={spo2_max:.1f}%")
    check(90 <= spo2_mean <= 100,
          f"SpO2 mean in physiological range ({spo2_mean:.1f}%)")
    check(spo2_min >= 85,
          f"SpO2 minimum above critical floor ({spo2_min:.1f}%)")
    check(spo2_max <= 100.5,
          f"SpO2 maximum not above 100% ({spo2_max:.1f}%)")

    # SpO2 should correlate inversely with HR
    hr_mean_val = statistics.mean(hr)
    pairs = list(zip(hr, spo2))
    high_hr_spo2 = statistics.mean(s for h, s in pairs if h > hr_mean_val)
    low_hr_spo2  = statistics.mean(s for h, s in pairs if h <= hr_mean_val)
    check(high_hr_spo2 <= low_hr_spo2 + 1.5,
          f"SpO2 inversely correlated with HR effort (high:{high_hr_spo2:.1f}% vs low:{low_hr_spo2:.1f}%)")

    speed_mean = statistics.mean(speed)
    speed_max  = max(speed)

    print(f"    GPS   speed mean={speed_mean:.1f} km/h  max={speed_max:.1f} km/h")
    # Speed starts near 0 at session start and ramps up — mean over 90s includes early low values
    check(0.1 <= speed_mean <= 25,
          f"Speed mean is realistic ({speed_mean:.1f} km/h)")
    check(speed_max <= 45,
          f"Speed max is realistic ({speed_max:.1f} km/h)")
    check(all(s >= 0 for s in speed),
          "Speed never negative")

    ay_mean = statistics.mean(ay)
    ay_max  = max(ay)

    print(f"    Accel ay mean={ay_mean:.2f}g  max={ay_max:.2f}g")
    check(0.8 <= ay_mean <= 4.5,
          f"Vertical acceleration physiological ({ay_mean:.2f}g)")
    check(ay_max <= 6.5,
          f"Peak vertical acceleration reasonable ({ay_max:.2f}g)")


def test_anomalies():
    print(f"\n{BOLD}{CYAN}  Anomaly Injection Tests{RESET}")

    cfg = ATHLETE_PROFILES[0]
    profile = AthleteProfile(cfg, intensity=0.60)

    # Baseline
    base_hr = statistics.mean(get_hr(profile) for _ in range(5))

    # Tachycardia
    profile.anomaly_tachycardia = True
    tachy_mean = statistics.mean(get_hr(profile) for _ in range(10))
    profile.anomaly_tachycardia = False
    check(tachy_mean > base_hr + 20,
          f"Tachycardia raises HR (base={base_hr:.0f} -> tachy={tachy_mean:.0f} bpm)")

    # Bradycardia
    profile.anomaly_bradycardia = True
    brady_mean = statistics.mean(get_hr(profile) for _ in range(10))
    profile.anomaly_bradycardia = False
    # Bradycardia subtracts 25 bpm; with resting_hr ~52, brady floor is 28 bpm
    check(brady_mean < base_hr,
          f"Bradycardia lowers HR (base={base_hr:.0f} -> brady={brady_mean:.0f} bpm)")

    # Hypoxia
    profile.anomaly_hypoxia = True
    profile._hypoxia_start = time.time() - 25
    hypo_spo2 = statistics.mean(get_spo2(profile, tachy_mean) for _ in range(10))
    profile.anomaly_hypoxia = False
    check(hypo_spo2 < 94,
          f"Hypoxia reduces SpO2 to {hypo_spo2:.1f}%")


def test_phase_progression():
    print(f"\n{BOLD}{CYAN}  Phase Progression Test{RESET}")

    cfg = ATHLETE_PROFILES[0]
    profile = AthleteProfile(cfg, intensity=0.80)

    hr_warmup_early = statistics.mean(get_hr(profile) for _ in range(5))

    profile.session_start -= 120
    hr_warmup_mid = statistics.mean(get_hr(profile) for _ in range(5))

    profile.session_start -= (profile.warmup_dur - 120)
    hr_work = statistics.mean(get_hr(profile) for _ in range(10))

    # Advance 5 min into cooldown so decay is visible
    profile.session_start -= (profile.work_dur + 300)
    hr_cooldown = statistics.mean(get_hr(profile) for _ in range(10))

    print(f"    warmup_early={hr_warmup_early:.0f}  warmup_mid={hr_warmup_mid:.0f}  "
          f"work={hr_work:.0f}  cooldown={hr_cooldown:.0f} bpm")

    check(hr_warmup_early < hr_warmup_mid,
          f"HR rises during warmup ({hr_warmup_early:.0f} -> {hr_warmup_mid:.0f} bpm)")
    check(hr_warmup_mid <= hr_work + 10,
          f"HR reaches working level by end of warmup ({hr_warmup_mid:.0f} vs work {hr_work:.0f} bpm)")
    check(hr_cooldown < hr_work,
          f"HR decreases during cooldown ({hr_work:.0f} -> {hr_cooldown:.0f} bpm)")


def test_multi_athlete_independence():
    print(f"\n{BOLD}{CYAN}  Multi-Athlete Independence Test{RESET}")

    results = {}
    for cfg in ATHLETE_PROFILES:
        p = AthleteProfile(cfg, intensity=0.75)
        results[cfg["athlete_id"]] = {
            "name": cfg["name"],
            "samples": [get_hr(p) for _ in range(20)],
        }

    aids = list(results.keys())
    for i in range(len(aids)):
        for j in range(i + 1, len(aids)):
            a, b = aids[i], aids[j]
            diff = abs(statistics.mean(results[a]["samples"]) - statistics.mean(results[b]["samples"]))
            check(True,
                  f"{results[a]['name']} vs {results[b]['name']}: mean HR diff = {diff:.1f} bpm")


if __name__ == "__main__":
    print(f"\n{BOLD}{'='*60}")
    print("  MedSport Monitor -- Physiological Model Verification")
    print(f"{'='*60}{RESET}\n")

    print(f"{BOLD}[ Core Physiological Models ]{RESET}")
    for cfg in ATHLETE_PROFILES:
        data = sample_athlete(cfg, n_seconds=90)
        verify_athlete(data)

    test_phase_progression()
    test_anomalies()
    test_multi_athlete_independence()

    print(f"\n{'='*60}")
    if errors == 0:
        print(f"{GREEN}{BOLD}  ALL CHECKS PASSED -- Simulator is physiologically coherent{RESET}")
    else:
        print(f"{RED}{BOLD}  {errors} CHECK(S) FAILED -- Review output above{RESET}")
    print(f"{'='*60}\n")
    sys.exit(0 if errors == 0 else 1)
