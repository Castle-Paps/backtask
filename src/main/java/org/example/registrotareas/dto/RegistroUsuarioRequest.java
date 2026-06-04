package org.example.registrotareas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroUsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 80)
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(min = 2, max = 80)
        String apellido,

        @NotBlank(message = "El usuario es obligatorio")
        @Size(min = 4, max = 60)
        String usuario,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 100)
        String contrasena
) {
}
