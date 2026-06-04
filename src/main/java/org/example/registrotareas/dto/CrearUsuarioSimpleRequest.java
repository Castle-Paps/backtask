package org.example.registrotareas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud simplificada para crear usuarios operativos.
 * Solo requiere usuario y contraseña — lo usa el administrador
 * desde el menú lateral de la app (sin nombre ni apellido).
 */
public record CrearUsuarioSimpleRequest(

        @NotBlank(message = "El usuario es obligatorio")
        @Size(min = 4, max = 60, message = "El usuario debe tener entre 4 y 60 caracteres")
        String usuario,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 4, max = 100, message = "La contraseña debe tener al menos 4 caracteres")
        String contrasena

) {}
