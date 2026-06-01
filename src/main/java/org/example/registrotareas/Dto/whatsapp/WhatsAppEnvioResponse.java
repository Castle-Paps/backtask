package org.example.registrotareas.dto.whatsapp;

public record WhatsAppEnvioResponse(
        Long tareaId,
        String telefono,
        String whatsappUrl,
        String mensaje,
        String estado,
        String detalle
) {
}
