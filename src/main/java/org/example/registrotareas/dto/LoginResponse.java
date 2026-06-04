package org.example.registrotareas.dto;

public record LoginResponse(
        String token,
        String usuario,
        String nombre
) {
}
