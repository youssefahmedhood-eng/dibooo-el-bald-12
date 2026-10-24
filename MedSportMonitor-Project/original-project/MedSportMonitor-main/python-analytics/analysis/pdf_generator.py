"""
Clinical PDF report generator — ReportLab + Matplotlib.
Produces a standardized MedSport Monitor clinical report.
"""

import base64
import io
import math
from datetime import datetime
from typing import Any, Optional

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm, mm
from reportlab.platypus import (
    HRFlowable, Image, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle,
)

# Brand colors
BRAND_PRIMARY  = colors.HexColor("#0A84FF")
BRAND_ACCENT   = colors.HexColor("#00D4FF")
BRAND_DARK     = colors.HexColor("#0D0D0D")
BRAND_GRAY     = colors.HexColor("#A0A0B0")
BRAND_RED      = colors.HexColor("#D32F2F")
BRAND_GREEN    = colors.HexColor("#00C853")

PAGE_W, PAGE_H = A4


def _make_hr_chart(hr_series: list[float], hr_max: float) -> Optional[io.BytesIO]:
    """Generates a HR timeline chart as PNG bytes."""
    if not hr_series:
        return None
    fig, ax = plt.subplots(figsize=(14, 3.5), facecolor="#0D0D0D")
    ax.set_facecolor("#0D0D0D")

    t = np.arange(len(hr_series))

    # Zone color fills
    reserve = hr_max - 50  # approximate
    z_bounds = [50 + reserve * r for r in [0.50, 0.60, 0.70, 0.80, 0.90, 1.05]]
    zone_colors = ["#607D8B", "#4CAF50", "#FFC107", "#FF9800", "#F44336"]
    for i, zc in enumerate(zone_colors):
        ax.axhspan(z_bounds[i], z_bounds[i+1], alpha=0.15, color=zc)

    ax.plot(t, hr_series, color="#0A84FF", linewidth=1.5, alpha=0.9)
    ax.fill_between(t, hr_series, min(hr_series), alpha=0.15, color="#0A84FF")
    ax.set_xlim(0, len(hr_series))
    ax.set_ylim(max(40, min(hr_series) - 10), min(hr_max * 1.05, max(hr_series) + 10))
    ax.tick_params(colors="#A0A0B0")
    ax.spines[:].set_color("#333")
    ax.set_xlabel("Time (s)", color="#A0A0B0", fontsize=8)
    ax.set_ylabel("HR (bpm)", color="#A0A0B0", fontsize=8)
    ax.set_title("Heart Rate — Session Timeline", color="white", fontsize=9, pad=8)
    plt.tight_layout(pad=0.5)

    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=120, bbox_inches="tight", facecolor=fig.get_facecolor())
    plt.close(fig)
    buf.seek(0)
    return buf


def generate_athlete_pdf(
        athlete_info: dict[str, Any],
        team_info: dict[str, Any],
        doctor_info: dict[str, Any],
        sessions: list[dict[str, Any]],
        soap_notes: list[dict[str, Any]],
        restriction: Optional[dict[str, Any]],
        risk_data: Optional[dict[str, Any]],
        period: str = "Last 30 days",
) -> bytes:
    """
    Generates a complete clinical PDF report and returns the raw bytes.
    """
    buf = io.BytesIO()
    doc = SimpleDocTemplate(
        buf, pagesize=A4,
        topMargin=2*cm, bottomMargin=2*cm,
        leftMargin=2*cm, rightMargin=2*cm,
    )

    styles = getSampleStyleSheet()
    story  = []

    h1 = ParagraphStyle("H1", fontName="Helvetica-Bold", fontSize=20, textColor=colors.white,
                         spaceAfter=4, leading=24)
    h2 = ParagraphStyle("H2", fontName="Helvetica-Bold", fontSize=12, textColor=BRAND_PRIMARY,
                         spaceAfter=6, spaceBefore=12)
    body = ParagraphStyle("Body", fontName="Helvetica", fontSize=9, textColor=colors.white,
                           leading=14, spaceAfter=4)
    small = ParagraphStyle("Small", fontName="Helvetica", fontSize=8, textColor=BRAND_GRAY,
                            leading=12)
    right = ParagraphStyle("Right", fontName="Helvetica", fontSize=8, textColor=BRAND_GRAY,
                            alignment=TA_RIGHT)

    # ---- HEADER ----
    header_data = [[
        Paragraph("<b>MedSport Monitor</b>", h1),
        Paragraph(f"<font color='#A0A0B0'>Clinical Report</font><br/>"
                  f"<font color='#A0A0B0'>{datetime.now().strftime('%d %B %Y')}</font>", right),
    ]]
    header_table = Table(header_data, colWidths=[10*cm, 7*cm])
    header_table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#0D0D0D")),
        ("VALIGN",     (0, 0), (-1, -1), "MIDDLE"),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    story.append(header_table)
    story.append(HRFlowable(width="100%", thickness=1, color=BRAND_PRIMARY))
    story.append(Spacer(1, 6*mm))

    # ---- ATHLETE + TEAM INFO ----
    story.append(Paragraph("Athlete Information", h2))
    info_rows = [
        ["Name",          athlete_info.get("name", "-"),
         "Team",          team_info.get("name", "-")],
        ["Date of Birth", athlete_info.get("birth_date", "-"),
         "Sport",         team_info.get("sport", "-")],
        ["Sex",           athlete_info.get("sex", "-"),
         "HR Max",        f"{athlete_info.get('hr_max', '-')} bpm"],
        ["Height",        f"{athlete_info.get('height_cm', '-')} cm",
         "Weight",        f"{athlete_info.get('weight_kg', '-')} kg"],
    ]
    info_table = Table(info_rows, colWidths=[3.5*cm, 5.5*cm, 3.5*cm, 4.5*cm])
    info_table.setStyle(TableStyle([
        ("FONTNAME",    (0, 0), (-1, -1), "Helvetica"),
        ("FONTSIZE",    (0, 0), (-1, -1), 9),
        ("TEXTCOLOR",   (0, 0), (-1, -1), colors.white),
        ("TEXTCOLOR",   (0, 0), (0, -1), BRAND_GRAY),   # label cols
        ("TEXTCOLOR",   (2, 0), (2, -1), BRAND_GRAY),
        ("BACKGROUND",  (0, 0), (-1, -1), colors.HexColor("#1A1A2E")),
        ("ROWBACKGROUNDS", (0, 0), (-1, -1), [colors.HexColor("#1A1A2E"), colors.HexColor("#141428")]),
        ("GRID",        (0, 0), (-1, -1), 0.3, colors.HexColor("#333")),
        ("PADDING",     (0, 0), (-1, -1), 6),
    ]))
    story.append(info_table)
    story.append(Spacer(1, 6*mm))

    # ---- MEDICAL RESTRICTION ----
    if restriction and restriction.get("status") == "active":
        story.append(Paragraph("Active Medical Restriction", h2))
        r_data = [
            ["Type",        restriction.get("type", "-"),
             "HR Limit",    f"{restriction.get('hr_limit', 'N/A')} bpm"],
            ["Start Date",  restriction.get("start_date", "-"),
             "Return Date", restriction.get("return_date", "TBD")],
            ["Description", restriction.get("description", "-"), "", ""],
        ]
        r_table = Table(r_data, colWidths=[3.5*cm, 5.5*cm, 3.5*cm, 4.5*cm])
        r_table.setStyle(TableStyle([
            ("FONTNAME",   (0, 0), (-1, -1), "Helvetica"),
            ("FONTSIZE",   (0, 0), (-1, -1), 9),
            ("TEXTCOLOR",  (0, 0), (-1, -1), colors.white),
            ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#2A0A0A")),
            ("LEFTBORDERPADDING", (0, 0), (-1, -1), 6),
            ("GRID",       (0, 0), (-1, -1), 0.3, colors.HexColor("#5A1A1A")),
            ("SPAN",       (1, 2), (3, 2)),
            ("PADDING",    (0, 0), (-1, -1), 6),
        ]))
        story.append(r_table)
        story.append(Spacer(1, 6*mm))

    # ---- CARDIOVASCULAR RISK ----
    if risk_data:
        story.append(Paragraph("Cardiovascular Risk Assessment", h2))
        ri = risk_data.get("risk_index", 0)
        rl = risk_data.get("risk_level", "LOW")
        rc = {
            "LOW": BRAND_GREEN, "MODERATE": colors.HexColor("#FFC107"),
            "HIGH": colors.HexColor("#FF9800"), "VERY_HIGH": BRAND_RED,
        }.get(rl, BRAND_GRAY)
        risk_rows = [
            [f"Risk Index: {ri}/100", f"Level: {rl}", f"Patterns: {len(risk_data.get('patterns', []))} detected"],
        ]
        rt = Table(risk_rows, colWidths=[5*cm, 4*cm, 8*cm])
        rt.setStyle(TableStyle([
            ("FONTNAME",   (0, 0), (-1, -1), "Helvetica-Bold"),
            ("FONTSIZE",   (0, 0), (-1, -1), 10),
            ("TEXTCOLOR",  (0, 0), (-1, -1), colors.white),
            ("TEXTCOLOR",  (1, 0), (1, 0),   rc),
            ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#1A1A2E")),
            ("PADDING",    (0, 0), (-1, -1), 8),
            ("GRID",       (0, 0), (-1, -1), 0.3, colors.HexColor("#333")),
        ]))
        story.append(rt)
        for p in risk_data.get("patterns", []):
            story.append(Spacer(1, 2*mm))
            story.append(Paragraph(
                f"<b>{p.get('type', '')}</b> — {p.get('description', '')}<br/>"
                f"<font color='#A0A0B0'>Recommendation: {p.get('recommendation', '')}</font>",
                body
            ))
        story.append(Spacer(1, 6*mm))

    # ---- SESSIONS SUMMARY ----
    if sessions:
        story.append(Paragraph(f"Training Sessions — {period}", h2))
        sess_header = ["Date", "Type", "Duration", "Avg HR", "Max HR", "Dist (km)", "Predominant Zone"]
        sess_rows = [sess_header]
        for s in sessions[:15]:  # cap at 15 to avoid overflow
            sess_rows.append([
                s.get("date", "-"),
                s.get("exercise_type", "-"),
                f"{s.get('duration_min', 0):.0f} min",
                f"{s.get('avg_hr', 0):.0f} bpm",
                f"{s.get('max_hr', 0):.0f} bpm",
                f"{s.get('distance_km', 0):.2f}",
                f"Z{s.get('predominant_zone', '-')}",
            ])
        st = Table(sess_rows, colWidths=[2.5*cm, 2.5*cm, 2*cm, 2.5*cm, 2.5*cm, 2.5*cm, 3*cm])
        st.setStyle(TableStyle([
            ("FONTNAME",      (0, 0), (-1, -1), "Helvetica"),
            ("FONTSIZE",      (0, 0), (-1, -1), 8),
            ("FONTNAME",      (0, 0), (-1, 0),  "Helvetica-Bold"),
            ("TEXTCOLOR",     (0, 0), (-1, -1), colors.white),
            ("TEXTCOLOR",     (0, 0), (-1, 0),  BRAND_ACCENT),
            ("BACKGROUND",    (0, 0), (-1, 0),  colors.HexColor("#1A1A2E")),
            ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.HexColor("#141428"), colors.HexColor("#1A1A2E")]),
            ("GRID",          (0, 0), (-1, -1), 0.3, colors.HexColor("#333")),
            ("PADDING",       (0, 0), (-1, -1), 5),
        ]))
        story.append(st)
        story.append(Spacer(1, 6*mm))

    # ---- SOAP NOTES ----
    if soap_notes:
        story.append(Paragraph("Clinical SOAP Notes", h2))
        for note in soap_notes[:5]:
            story.append(Paragraph(
                f"<b>Date:</b> {note.get('date', '-')} | <b>Author:</b> {note.get('doctor_name', '-')}",
                small
            ))
            for field, label in [("subjective","S"), ("objective","O"),
                                   ("assessment","A"), ("plan","P")]:
                val = note.get(field, "")
                if val:
                    story.append(Paragraph(
                        f"<font color='#00D4FF'><b>[{label}]</b></font> {val}", body
                    ))
            story.append(HRFlowable(width="100%", thickness=0.3, color=colors.HexColor("#333")))
            story.append(Spacer(1, 3*mm))

    # ---- FOOTER ----
    story.append(Spacer(1, 8*mm))
    story.append(HRFlowable(width="100%", thickness=1, color=BRAND_GRAY))
    story.append(Spacer(1, 3*mm))
    footer_text = (
        f"<font color='#A0A0B0'>Signed by: {doctor_info.get('name', '-')} | "
        f"License: {doctor_info.get('license', '-')} | "
        f"Specialty: {doctor_info.get('specialty', 'Sports Medicine')} | "
        f"Generated by MedSport Monitor v1.0</font>"
    )
    story.append(Paragraph(footer_text, small))

    doc.build(story)
    return buf.getvalue()
