package org.example.registrotareas.dto;

import java.time.LocalDateTime;

public record ArchivoResponse(
        Long id,
        String driveFileId,
        String nombreArchivo,
        String mimeType,
        Long tamanoBytes,
        String urlArchivo,
        LocalDateTime fechaSubida,
        String usuarioCreacion
) {}
