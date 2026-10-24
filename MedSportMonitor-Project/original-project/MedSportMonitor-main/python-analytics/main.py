"""
MedSport Monitor — FastAPI Analytics Microservice
Runs independently on localhost:8000

Usage:
    uvicorn main:app --reload --port 8000
"""

import base64
from typing import Any, Optional

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from analysis.heart_zones import analyze_zones
from analysis.fatigue_index import compute_fatigue_index
from analysis.radar_metrics import compute_radar_metrics
from analysis.cardiovascular_risk import compute_cardiovascular_risk
from analysis.hrv_analysis import compute_hrv, compute_training_load
from analysis.training_load import compute_team_load
from analysis.pdf_generator import generate_athlete_pdf

app = FastAPI(
    title="MedSport Monitor Analytics",
    description="Physiological analysis microservice for MedSport Monitor",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ============================================================
# REQUEST MODELS
# ============================================================

class HeartZonesRequest(BaseModel):
    athlete_id: str
    session_id: Optional[str] = None
    hr_series: list[float]
    hr_max: Optional[float] = 190.0
    resting_hr: Optional[float] = 55.0
    sample_interval_s: Optional[float] = 1.0


class FatigueIndexRequest(BaseModel):
    athlete_id: str
    last_7_days_sessions: list[dict[str, Any]]


class RadarMetricsRequest(BaseModel):
    athlete_id: str
    sessions: list[dict[str, Any]]


class CardioRiskRequest(BaseModel):
    athlete_id: str
    biometrics: dict[str, Any]
    last_sessions: list[dict[str, Any]]


class TrainingLoadRequest(BaseModel):
    team_id: str
    athletes: list[dict[str, Any]]
    period_days: Optional[int] = 7


class SessionSummaryRequest(BaseModel):
    session_id: str
    all_sensor_data: dict[str, Any]


class AthletePdfRequest(BaseModel):
    athlete_id: str
    period: Optional[str] = "Last 30 days"
    include_soap: Optional[bool] = True
    doctor_info: dict[str, str]
    team_info: dict[str, str]
    athlete_info: Optional[dict[str, Any]] = None
    sessions: Optional[list[dict[str, Any]]] = None
    soap_notes: Optional[list[dict[str, Any]]] = None
    restriction: Optional[dict[str, Any]] = None
    risk_data: Optional[dict[str, Any]] = None


class SessionPdfRequest(BaseModel):
    session_id: str
    doctor_info: dict[str, str]
    session_data: Optional[dict[str, Any]] = None


# ============================================================
# HEALTH
# ============================================================

@app.get("/api/health")
def health():
    return {"status": "ok", "version": "1.0.0", "service": "MedSport Analytics"}


# ============================================================
# ANALYSIS ENDPOINTS
# ============================================================

@app.post("/api/analysis/heart-zones")
def heart_zones(req: HeartZonesRequest):
    if not req.hr_series:
        raise HTTPException(400, "hr_series cannot be empty")
    result = analyze_zones(
        req.hr_series,
        req.hr_max or 190.0,
        req.resting_hr or 55.0,
        req.sample_interval_s or 1.0,
    )
    # Enrich with HRV
    hrv = compute_hrv(req.hr_series)
    result["hrv"] = hrv
    return result


@app.post("/api/analysis/fatigue-index")
def fatigue_index(req: FatigueIndexRequest):
    return compute_fatigue_index(req.last_7_days_sessions)


@app.post("/api/analysis/radar-metrics")
def radar_metrics(req: RadarMetricsRequest):
    return compute_radar_metrics(req.sessions)


@app.post("/api/analysis/cardiovascular-risk")
def cardiovascular_risk(req: CardioRiskRequest):
    return compute_cardiovascular_risk(req.biometrics, req.last_sessions)


@app.post("/api/analysis/training-load")
def training_load(req: TrainingLoadRequest):
    return compute_team_load(req.athletes, req.period_days or 7)


@app.post("/api/analysis/session-summary")
def session_summary(req: SessionSummaryRequest):
    """Summarizes a completed session from raw sensor data."""
    data = req.all_sensor_data
    hr_series    = data.get("hr_series", [])
    speed_series = data.get("speed_series", [])
    hr_max       = data.get("hr_max", 190.0)
    resting_hr   = data.get("resting_hr", 55.0)

    zones = analyze_zones(hr_series, hr_max, resting_hr) if hr_series else {}
    hrv   = compute_hrv(hr_series) if hr_series else {}

    avg_hr  = sum(hr_series) / len(hr_series) if hr_series else 0
    max_hr  = max(hr_series) if hr_series else 0
    avg_spd = sum(speed_series) / len(speed_series) if speed_series else 0
    max_spd = max(speed_series) if speed_series else 0

    return {
        "session_id": req.session_id,
        "avg_hr": round(avg_hr, 1),
        "max_hr": round(max_hr, 1),
        "avg_speed_kmh": round(avg_spd, 2),
        "max_speed_kmh": round(max_spd, 2),
        "zones": zones,
        "hrv": hrv,
        "anomalies": [],  # extended anomaly detection can be added here
    }


# ============================================================
# REPORT ENDPOINTS
# ============================================================

@app.post("/api/reports/generate-athlete-pdf")
def generate_athlete_pdf_endpoint(req: AthletePdfRequest):
    """Generates a clinical PDF report for an athlete."""
    try:
        pdf_bytes = generate_athlete_pdf(
            athlete_info=req.athlete_info or {"name": f"Athlete {req.athlete_id}"},
            team_info=req.team_info,
            doctor_info=req.doctor_info,
            sessions=req.sessions or [],
            soap_notes=req.soap_notes or [] if req.include_soap else [],
            restriction=req.restriction,
            risk_data=req.risk_data,
            period=req.period or "Last 30 days",
        )
        return {
            "pdf_base64": base64.b64encode(pdf_bytes).decode("utf-8"),
            "size_bytes": len(pdf_bytes),
        }
    except Exception as e:
        raise HTTPException(500, f"PDF generation failed: {e}")


@app.post("/api/reports/generate-session-pdf")
def generate_session_pdf_endpoint(req: SessionPdfRequest):
    """Generates a session-specific clinical PDF report."""
    try:
        session_data = req.session_data or {}
        pdf_bytes = generate_athlete_pdf(
            athlete_info=session_data.get("athlete_info", {}),
            team_info=session_data.get("team_info", {}),
            doctor_info=req.doctor_info,
            sessions=[session_data] if session_data else [],
            soap_notes=[],
            restriction=None,
            risk_data=None,
            period=f"Session {req.session_id}",
        )
        return {
            "pdf_base64": base64.b64encode(pdf_bytes).decode("utf-8"),
            "size_bytes": len(pdf_bytes),
        }
    except Exception as e:
        raise HTTPException(500, f"Session PDF generation failed: {e}")


@app.post("/api/reports/generate-team-epidemiology")
def generate_team_epidemiology(payload: dict[str, Any]):
    """
    Generates an anonymized team epidemiology report.
    Returns metadata only — actual PDF generation uses generate_athlete_pdf internally.
    """
    team_id   = payload.get("team_id", "")
    period    = payload.get("period", "Last 30 days")
    athletes  = payload.get("athletes", [])

    # Anonymize
    anonymized = []
    for i, a in enumerate(athletes):
        anonymized.append({
            "label": f"Athlete {chr(65+i)}",
            "sessions": a.get("sessions", []),
            "restrictions": a.get("restrictions", []),
            "alerts": a.get("alerts", []),
        })

    return {
        "team_id": team_id,
        "period": period,
        "athlete_count": len(athletes),
        "anonymized": anonymized,
        "note": "Use /api/reports/generate-athlete-pdf for individual PDF generation.",
    }
