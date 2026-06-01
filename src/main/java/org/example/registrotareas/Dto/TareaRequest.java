package org.example.registrotareas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TareaRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(min = 2, max = 100)
        String nombreCliente,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^[0-9]{9,15}$", message = "El teléfono debe tener entre 9 y 15 dígitos")
        String telefono,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 5, max = 500)
        String motivo,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        BigDecimal monto,

        @DecimalMin(value = "0.00", message = "El adelanto no puede ser negativo")
        BigDecimal adelanto,

        @Min(value = 1, message = "El tiempo estimado debe ser al menos 1 día")
        Integer tiempoDias
) {
}
