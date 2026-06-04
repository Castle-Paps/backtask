package org.example.registrotareas.dto;

public record UsuarioResponse(
        Long id,
        String nombre,
        String apellido,
        String usuario,
        String rol,
        boolean enabled
) {
}
