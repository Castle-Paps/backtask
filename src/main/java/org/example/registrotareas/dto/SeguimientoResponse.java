package org.example.registrotareas.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SeguimientoResponse(
        Long id,
        Long tareaId,
        String tipo,
        String descripcion,
        String usuario,
        LocalDateTime fechaCreacion,
        List<ArchivoResponse> archivos    // archivos de evidencia en Google Drive
) {
}
