package org.example.registrotareas.service;

import org.example.registrotareas.entity.Estado;
import org.example.registrotareas.entity.Tarea;
import org.example.registrotareas.service.impl.MensajeWhatsappServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MensajeWhatsappServiceImplTest {

    private final MensajeWhatsappServiceImpl service = new MensajeWhatsappServiceImpl();

    private Tarea tareaBase() {
        Tarea tarea = new Tarea();
        tarea.setNombreCliente("Juan Pérez");
        tarea.setTelefono("51987654321");
        tarea.setMotivo("Reparación de pantalla");
        tarea.setMonto(new BigDecimal("200.00"));
        tarea.setAdelanto(new BigDecimal("80.00"));
        tarea.setTiempoDias(3);
        tarea.setNumeroTicket("TK-000001");
        tarea.setEstado(Estado.PENDIENTE);
        return tarea;
    }

    @Test
    void resumenCreacionContieneLosDatosClaves() {
        String resumen = service.construirResumenCreacion(tareaBase());

        assertThat(resumen).contains("TK-000001");
        assertThat(resumen).contains("Juan Pérez");
        assertThat(resumen).contains("Reparación de pantalla");
        assertThat(resumen).contains("3 días");
        assertThat(resumen).contains("Gracias por confiar en nosotros");
    }

    @Test
    void mensajeCompletadoContieneEstadoYDescripcion() {
        String mensaje = service.construirMensajeCompletado(
                tareaBase(), "Pantalla reemplazada exitosamente", List.of());

        assertThat(mensaje).contains("TK-000001");
        assertThat(mensaje).contains("COMPLETADO");
        assertThat(mensaje).contains("Pantalla reemplazada exitosamente");
        assertThat(mensaje).contains("Gracias por confiar en nosotros");
    }

    @Test
    void mensajeCanceladoContieneMotivo() {
        String mensaje = service.construirMensajeCancelado(
                tareaBase(), "El cliente no recogió el equipo");

        assertThat(mensaje).contains("TK-000001");
        assertThat(mensaje).contains("CANCELADO");
        assertThat(mensaje).contains("El cliente no recogió el equipo");
    }

    @Test
    void urlWhatsAppUsaWaMe() {
        String url = service.construirUrlWhatsApp("51987654321", "Hola mundo");

        assertThat(url).startsWith("https://wa.me/51987654321?text=");
        assertThat(url).contains("Hola");
    }

    @Test
    void totalEsMontoMenosAdelanto() {
        Tarea tarea = tareaBase(); // monto=200, adelanto=80 → total=120
        String resumen = service.construirResumenCreacion(tarea);
        // El resumen incluye el total (monto - adelanto)
        assertThat(resumen).contains("120");
    }
}
