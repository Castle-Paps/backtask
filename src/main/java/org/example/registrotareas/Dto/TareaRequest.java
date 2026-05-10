package org.example.registrotareas.Dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.registrotareas.Entidades.Estado;
import org.example.registrotareas.Entidades.MetodoPago;

import java.math.BigDecimal;

public record TareaRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre del cliente debe tener entre 2 y 100 caracteres")
        String nombreCliente,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^[0-9]{9,15}$", message = "El teléfono debe contener solo números y tener entre 9 y 15 dígitos")
        String telefono,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 5, max = 500, message = "El motivo debe tener entre 5 y 500 caracteres")
        String motivo,

        @NotNull(message = "El total es obligatorio")
        @DecimalMin(value = "0.01", message = "El total debe ser mayor a 0")
        BigDecimal total,

        @NotNull(message = "El adelanto es obligatorio")
        @DecimalMin(value = "0.00", message = "El adelanto no puede ser negativo")
        BigDecimal adelanto,

        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodoPago
) {
}
