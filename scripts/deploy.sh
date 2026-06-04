#!/bin/bash
# ──────────────────────────────────────────────────────────────────────────────
# deploy.sh — Actualizar el backend sin tiempo de inactividad
# Uso: ./scripts/deploy.sh
# ──────────────────────────────────────────────────────────────────────────────

set -e

PROYECTO_DIR="/opt/registrotareas"
cd "$PROYECTO_DIR"

echo "======================================================"
echo " RegistroTareas — Deploy $(date)"
echo "======================================================"

# 1. Obtener últimos cambios del código
echo "[1/5] Actualizando código..."
git pull --ff-only

# 2. Backup preventivo antes de actualizar
echo "[2/5] Backup de seguridad..."
./scripts/backup.sh

# 3. Reconstruir la imagen del backend
echo "[3/5] Compilando imagen Docker..."
docker compose build app

# 4. Reiniciar solo el backend (DB y cloudflared siguen corriendo)
echo "[4/5] Reiniciando backend..."
docker compose up -d --no-deps app

# 5. Verificar que arrancó correctamente
echo "[5/5] Verificando salud del servicio..."
sleep 15
if docker exec registrotareas_app wget -qO- http://localhost:8088/api/health > /dev/null 2>&1; then
    echo ""
    echo "✅ Deploy exitoso. Backend activo en task.yzulatecperu.uk"
else
    echo ""
    echo "❌ El backend no respondió. Revisando logs..."
    docker compose logs --tail=50 app
    exit 1
fi

echo ""
echo "Estado de contenedores:"
docker compose ps
