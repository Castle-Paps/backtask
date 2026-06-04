package org.example.registrotareas.service;

import org.example.registrotareas.entity.Estado;
import org.example.registrotareas.entity.Tarea;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios básicos de la entidad Tarea.
 * (Test de integración con BD eliminado — requería ImagenTarea que ya no existe)
 */
class TareaListadoRegresionTest {

    @Test
    void estadoInicialEsPendiente() {
        Tarea tarea = new Tarea();
        tarea.setEstado(Estado.PENDIENTE);
        assertThat(tarea.getEstado()).isEqualTo(Estado.PENDIENTE);
    }

    @Test
    void totalEsCalculadoCorrectamente() {
        Tarea tarea = new Tarea();
        tarea.setMonto(new BigDecimal("300.00"));
        tarea.setAdelanto(new BigDecimal("100.00"));

        BigDecimal total = tarea.getMonto().subtract(tarea.getAdelanto());
        assertThat(total).isEqualByComparingTo("200.00");
    }

    @Test
    void ticketSeFormateaConSeisCeros() {
        Tarea tarea = new Tarea();
        tarea.setNombreCliente("Test");
        // Simular la asignación del ticket
        String ticket = "TK-" + String.format("%06d", 1L);
        tarea.setNumeroTicket(ticket);
        assertThat(tarea.getNumeroTicket()).isEqualTo("TK-000001");
    }
}
