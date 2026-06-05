package org.example.registrotareas.dto;

public record LoginResponse(
        String token,
        String usuario,
        String nombre,
        String rol      // ROLE_ADMIN | ROLE_USER → la app usa esto para mostrar opciones
) {
}
