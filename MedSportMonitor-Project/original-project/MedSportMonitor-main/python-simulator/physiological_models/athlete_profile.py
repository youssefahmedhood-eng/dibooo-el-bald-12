"""Athlete physiological profile — per-athlete state machine."""

import math
import time
from config import WARMUP_DURATION, WORK_DURATION, COOLDOWN_DURATION


class AthleteProfile:
    """Encapsulates the physiological state for one athlete during a simulated session."""

    def __init__(self, config: dict, intensity: float = 0.75):
        self.athlete_id   = config["athlete_id"]
        self.age          = config["age"]
        self.sex          = config["sex"]
        self.resting_hr   = config["resting_hr"]
        self.hr_max       = config["hr_max"]
        self.weight_kg    = config["weight_kg"]
        self.height_cm    = config["height_cm"]
        self.fitness      = config["fitness_level"]  # 0-1
        self.sport        = config.get("sport", "running")

        # Intensity slider (0-1) — controlled externally by coach panel
        self.intensity    = intensity

        # Session phase timing
        self.session_start = time.time()
        self.warmup_dur    = WARMUP_DURATION
        self.work_dur      = WORK_DURATION
        self.cooldown_dur  = COOLDOWN_DURATION

        # Anomaly flags (activated from coach panel)
        self.anomaly_tachycardia  = False
        self.anomaly_bradycardia  = False
        self.anomaly_hypoxia      = False
        self.anomaly_irregularity = False
        self.anomaly_collapse     = False

        # Warmup time constant (seconds) — higher = slower rise
        self.tau_warmup   = 150 + (1 - self.fitness) * 60

        # Recovery rate constant — fitter athletes recover faster
        self.k_recovery   = 0.025 + self.fitness * 0.025

    @property
    def elapsed(self) -> float:
        return time.time() - self.session_start

    @property
    def phase(self) -> str:
        t = self.elapsed
        if t < self.warmup_dur:
            return "warmup"
        elif t < self.warmup_dur + self.work_dur:
            return "work"
        elif t < self.warmup_dur + self.work_dur + self.cooldown_dur:
            return "cooldown"
        else:
            return "rest"

    @property
    def target_work_hr(self) -> float:
        """Target HR during sustained work phase."""
        return self.resting_hr + self.intensity * (self.hr_max - self.resting_hr)

    def get_phase_progress(self) -> float:
        """0.0 → 1.0 progress within current phase."""
        t = self.elapsed
        if self.phase == "warmup":
            return min(1.0, t / self.warmup_dur)
        elif self.phase == "work":
            return min(1.0, (t - self.warmup_dur) / self.work_dur)
        elif self.phase == "cooldown":
            return min(1.0, (t - self.warmup_dur - self.work_dur) / self.cooldown_dur)
        return 1.0

    def get_expected_hr_base(self) -> float:
        """Noiseless base HR for the current phase and elapsed time."""
        t = self.elapsed
        target = self.target_work_hr

        if self.phase == "warmup":
            # Exponential rise toward target
            return self.resting_hr + (target - self.resting_hr) * (1 - math.exp(-t / self.tau_warmup))

        elif self.phase == "work":
            # Sustained work with drift compensation
            work_t = t - self.warmup_dur
            drift = math.sin(work_t / 300) * 3  # ±3 bpm slow oscillation
            return target + drift

        elif self.phase == "cooldown":
            cool_t = t - self.warmup_dur - self.work_dur
            return target * math.exp(-self.k_recovery * cool_t) + \
                   self.resting_hr * (1 - math.exp(-self.k_recovery * cool_t))

        else:
            return self.resting_hr
