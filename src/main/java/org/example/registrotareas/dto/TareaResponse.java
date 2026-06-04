package org.example.registrotareas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TareaResponse(
        Long id,
        String numeroTicket,
        String nombreCliente,
        String telefono,
        String motivo,
        BigDecimal monto,
        BigDecimal adelanto,
        BigDecimal total,
        String estado,
        String usuarioRegistro,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaEdicion,
        LocalDateTime fechaFinalizacion,
        LocalDate fechaVencimiento,
        Integer tiempoDias,
        Integer diasRestantes,
        String colorUrgencia,
        List<ArchivoResponse> archivos,    // archivos en Google Drive
        String whatsappUrl,
        String resumenWhatsapp,
        List<SeguimientoResponse> historial
) {
}
