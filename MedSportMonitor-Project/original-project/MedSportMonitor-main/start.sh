#!/usr/bin/env bash
# MedSport Monitor — macOS/Linux Startup Script

set -euo pipefail

MEDSPORT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_SIM="$MEDSPORT_ROOT/python-simulator"
PYTHON_API="$MEDSPORT_ROOT/python-analytics"
JAVA_APP="$MEDSPORT_ROOT/java-app"
LOG_DIR="$MEDSPORT_ROOT/logs"
PIDS_FILE="$MEDSPORT_ROOT/.pids"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}  ✓ $*${NC}"; }
warn() { echo -e "${YELLOW}  ! $*${NC}"; }
err()  { echo -e "${RED}  ✗ $*${NC}"; exit 1; }

cleanup() {
    echo -e "\nShutting down services..."
    if [[ -f "$PIDS_FILE" ]]; then
        while read -r pid; do
            kill "$pid" 2>/dev/null || true
        done < "$PIDS_FILE"
        rm -f "$PIDS_FILE"
    fi
    echo -e "${GREEN}All services stopped. Goodbye!${NC}"
}
trap cleanup EXIT INT TERM

mkdir -p "$LOG_DIR" "$MEDSPORT_ROOT/mosquitto_data"
> "$PIDS_FILE"

echo -e "${GREEN}=============================================${NC}"
echo -e "${GREEN}  MedSport Monitor — Starting System${NC}"
echo -e "${GREEN}=============================================${NC}"
echo

# ---- 1. Mosquitto ----
echo "[1/6] Checking Mosquitto MQTT broker..."
if ! command -v mosquitto &>/dev/null; then
    warn "Mosquitto not found."
    warn "Install: brew install mosquitto (Mac) | sudo apt install mosquitto (Ubuntu)"
    warn "Continuing without live MQTT data..."
else
    mosquitto -c "$MEDSPORT_ROOT/mosquitto.conf" -v > "$LOG_DIR/mosquitto.log" 2>&1 &
    echo $! >> "$PIDS_FILE"
    sleep 0.5
    ok "Mosquitto started on port 1883"
fi

# ---- 2. Python venv ----
echo
echo "[2/6] Setting up Python environment..."
if [[ ! -d "$MEDSPORT_ROOT/venv" ]]; then
    python3 -m venv "$MEDSPORT_ROOT/venv" || err "Python 3 not found. Install Python 3.11+"
    warn "Virtual environment created"
fi
source "$MEDSPORT_ROOT/venv/bin/activate"

# ---- 3. Dependencies ----
echo
echo "[3/6] Installing Python dependencies..."
pip install -r "$PYTHON_SIM/requirements.txt" -q
pip install -r "$PYTHON_API/requirements.txt" -q
ok "Python dependencies ready"

# ---- 4. FastAPI ----
echo
echo "[4/6] Starting FastAPI analytics service..."
(cd "$PYTHON_API" && uvicorn main:app --host 0.0.0.0 --port 8000 --log-level warning > "$LOG_DIR/fastapi.log" 2>&1) &
echo $! >> "$PIDS_FILE"
sleep 2
ok "FastAPI running at http://localhost:8000"

# ---- 5. Simulator ----
echo
echo "[5/6] Starting physiological simulator..."
(cd "$PYTHON_SIM" && python simulator.py > "$LOG_DIR/simulator.log" 2>&1) &
echo $! >> "$PIDS_FILE"
sleep 1
ok "Simulator started (publishing to MQTT)"

# ---- 6. Java app ----
echo
echo "[6/6] Building and launching Java application..."
cd "$JAVA_APP"
if command -v mvn &>/dev/null; then
    mvn clean package -q -DskipTests
elif [[ -f "./mvnw" ]]; then
    chmod +x mvnw && ./mvnw clean package -q -DskipTests
else
    err "Maven not found. Install from https://maven.apache.org"
fi

echo
echo -e "${GREEN}=============================================${NC}"
echo -e "${GREEN}  MedSport Monitor is running!${NC}"
echo -e "${GREEN}  Demo credentials (password: Demo1234!):${NC}"
echo -e "${GREEN}    Coach:   coach@demo.com${NC}"
echo -e "${GREEN}    Medical: doctor@demo.com${NC}"
echo -e "${GREEN}    Athlete: athlete1@demo.com${NC}"
echo -e "${GREEN}=============================================${NC}"
echo

export MAVEN_OPTS="-Dprism.order=sw -Xmx512m"
if [[ -f "./mvnw" ]]; then
    ./mvnw javafx:run
elif command -v mvn &>/dev/null; then
    mvn javafx:run
else
    err "Maven not found. Install from https://maven.apache.org"
fi
