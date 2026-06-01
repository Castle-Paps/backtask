package org.example.registrotareas.dto;

public record ImagenResponse(
        Long id,
        String nombreArchivo,
        String urlImagen,
        Long tamanoBytes
) {
}
