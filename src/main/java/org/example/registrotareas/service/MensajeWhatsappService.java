package org.example.registrotareas.service;

import org.example.registrotareas.entity.Tarea;

import java.util.List;

public interface MensajeWhatsappService {

    String construirResumenCreacion(Tarea tarea);

    String construirMensajeCompletado(Tarea tarea, String descripcionFinal, List<String> imagenesUrl);

    String construirMensajeCancelado(Tarea tarea, String motivo);

    String construirUrlWhatsApp(String telefono, String mensaje);
}
