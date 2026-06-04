package org.example.registrotareas.service.impl;

import org.example.registrotareas.entity.Tarea;
import org.example.registrotareas.service.MensajeWhatsappService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class MensajeWhatsappServiceImpl implements MensajeWhatsappService {

    @Override
    public String construirResumenCreacion(Tarea tarea) {
        return """
                Ticket #%s

                Cliente: %s
                Motivo: %s

                Tiempo estimado:
                %s

                Total:
                S/ %s

                Gracias por confiar en nosotros.
                Le informaremos cualquier avance relacionado con su servicio."""
                .formatted(
                        textoSeguro(tarea.getNumeroTicket()),
                        textoSeguro(tarea.getNombreCliente()),
                        textoSeguro(tarea.getMotivo()),
                        tiempoEstimado(tarea),
                        dinero(calcularTotal(tarea))
                );
    }

    @Override
    public String construirMensajeCompletado(Tarea tarea, String descripcionFinal, List<String> imagenesUrl) {
        return """
                Ticket #%s

                Cliente: %s
                Estado: COMPLETADO ✓

                %s

                Total: S/ %s

                Gracias por confiar en nosotros."""
                .formatted(
                        textoSeguro(tarea.getNumeroTicket()),
                        textoSeguro(tarea.getNombreCliente()),
                        textoSeguro(descripcionFinal),
                        dinero(calcularTotal(tarea))
                ).trim();
    }

    @Override
    public String construirMensajeCancelado(Tarea tarea, String motivo) {
        return """
                Ticket #%s

                Cliente: %s
                Estado: CANCELADO

                Motivo:
                %s

                Disculpe las molestias."""
                .formatted(
                        textoSeguro(tarea.getNumeroTicket()),
                        textoSeguro(tarea.getNombreCliente()),
                        textoSeguro(motivo)
                );
    }

    @Override
    public String construirUrlWhatsApp(String telefono, String mensaje) {
        return "https://wa.me/" + telefono + "?text="
                + URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
    }

    private BigDecimal calcularTotal(Tarea tarea) {
        BigDecimal monto = valorSeguro(tarea.getMonto());
        BigDecimal adelanto = valorSeguro(tarea.getAdelanto());
        return monto.subtract(adelanto);
    }

    private String tiempoEstimado(Tarea tarea) {
        Integer dias = tarea.getTiempoDias();
        if (dias == null || dias <= 0) return "Por confirmar";
        return dias + (dias == 1 ? " día" : " días");
    }

    private BigDecimal valorSeguro(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private String dinero(BigDecimal valor) {
        BigDecimal v = valorSeguro(valor).setScale(2, RoundingMode.HALF_UP);
        if (v.stripTrailingZeros().scale() <= 0) {
            return v.toBigInteger().toString();
        }
        return v.toPlainString();
    }

    private String textoSeguro(String valor) {
        return (valor == null || valor.isBlank()) ? "No especificado" : valor;
    }
}
