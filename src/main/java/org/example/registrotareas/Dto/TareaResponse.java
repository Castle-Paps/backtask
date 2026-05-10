package org.example.registrotareas.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TareaResponse(
        Long id,
        String nombreCliente,
        String telefono,
        String motivo,
        BigDecimal costo,
        BigDecimal adelanto,
        String proceso,
        String metodoPago,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaFinalizacion,
        List<ImagenResponse> imagenes,
        String resumenWhatsapp
) {
}
