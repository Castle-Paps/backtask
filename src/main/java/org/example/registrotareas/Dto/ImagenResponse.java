package org.example.registrotareas.Dto;

public record ImagenResponse(
        Long id,
        String nombreArchivo,
        String urlImagen,
        Long tamanoBytes
) {
}
