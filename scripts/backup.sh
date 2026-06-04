#!/bin/bash
# ──────────────────────────────────────────────────────────────────────────────
# backup.sh — Copia de seguridad diaria de PostgreSQL
# Instalar como cron: crontab -e
#   0 2 * * * /opt/registrotareas/scripts/backup.sh >> /opt/registrotareas/logs/backup.log 2>&1
# ──────────────────────────────────────────────────────────────────────────────

set -e

PROYECTO_DIR="/opt/registrotareas"
BACKUP_DIR="$PROYECTO_DIR/backups"
FECHA=$(date +%Y%m%d_%H%M%S)
ARCHIVO="$BACKUP_DIR/db_$FECHA.sql.gz"
RETENER_DIAS=7

cd "$PROYECTO_DIR"

mkdir -p "$BACKUP_DIR"

echo "[$(date)] Iniciando backup..."

# Volcar la base de datos desde el contenedor y comprimir
docker exec registrotareas_db \
    pg_dump -U postgres tareas \
    | gzip > "$ARCHIVO"

echo "[$(date)] Backup guardado: $ARCHIVO ($(du -sh "$ARCHIVO" | cut -f1))"

# Eliminar backups de más de N días
find "$BACKUP_DIR" -name "db_*.sql.gz" -mtime +$RETENER_DIAS -delete

echo "[$(date)] Backups disponibles:"
ls -lh "$BACKUP_DIR"/*.sql.gz 2>/dev/null || echo "  (ninguno)"

echo "[$(date)] Backup completado."
