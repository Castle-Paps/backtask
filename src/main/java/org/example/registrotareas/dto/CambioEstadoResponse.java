package org.example.registrotareas.dto;

public record CambioEstadoResponse(
        TareaResponse tarea,
        String whatsappUrl,
        String mensaje
) {
}
